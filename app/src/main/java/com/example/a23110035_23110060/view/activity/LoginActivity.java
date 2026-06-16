package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.controller.SessionManager;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "SUPABASE_LOGIN";
    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;
    private ImageView ivPasswordToggle;
    private OkHttpClient client;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SessionManager session = new SessionManager(this);
        if (session.getAccessToken() != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);

        client  = new OkHttpClient();

        // Navigate to SignUp
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Toggle Password Visibility
        ivPasswordToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_eye); // Assuming ic_eye is the open eye
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivPasswordToggle.setImageResource(R.drawable.ic_eye); // Update with closed eye if available
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Sign In Button Click
        btnSignIn.setOnClickListener(v -> performSignIn());
    }

    private void performSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        signInWithSupabase(email, password);
    }

    private void signInWithSupabase(String email, String password) {
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY;

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseAnonKey == null || supabaseAnonKey.isEmpty()) {
            Toast.makeText(this, "Supabase configuration missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Construction Error", e);
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer " + supabaseAnonKey)
                .post(body)
                .build();

        btnSignIn.setEnabled(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network Error: " + e.getMessage());
                runOnUiThread(() -> {
                    btnSignIn.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String accessToken = jsonResponse.optString("access_token");
                        String refreshToken = jsonResponse.optString("refresh_token");
                        JSONObject userJson = jsonResponse.getJSONObject("user");
                        String userId = userJson.getString("id");
                        String userEmail = userJson.optString("email", email);

                        // Extract full name from user_metadata first as fallback
                        JSONObject userMetadata = userJson.optJSONObject("user_metadata");
                        String metaName = null;
                        if (userMetadata != null) {
                            metaName = userMetadata.optString("full_name", null);
                        }
                        if (metaName == null || metaName.isEmpty() || metaName.equals("null")) {
                            metaName = userEmail;
                        }
                        final String finalMetaName = metaName;

                        // Query profiles table for full_name using user's access token
                        String profileUrl = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=full_name,email";
                        Request profileRequest = new Request.Builder()
                                .url(profileUrl)
                                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .get()
                                .build();

                        client.newCall(profileRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                handleLoginSuccess(userId, finalMetaName, accessToken, refreshToken);
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                String resolvedName = finalMetaName;
                                boolean profileExists = false;
                                if (response.isSuccessful() && response.body() != null) {
                                    try {
                                        JSONArray array = new JSONArray(response.body().string());
                                        if (array.length() > 0) {
                                            profileExists = true;
                                            JSONObject profile = array.getJSONObject(0);
                                            String dbName = profile.optString("full_name", null);
                                            if (dbName != null && !dbName.isEmpty() && !dbName.equals("null")) {
                                                resolvedName = dbName;
                                            }
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Profile Parse Error", e);
                                    }
                                }

                                if (!profileExists) {
                                    upsertProfile(userId, userEmail, resolvedName, accessToken);
                                }

                                handleLoginSuccess(userId, resolvedName, accessToken, refreshToken);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage());
                        runOnUiThread(() -> btnSignIn.setEnabled(true));
                    }
                } else {
                    runOnUiThread(() -> {
                        btnSignIn.setEnabled(true);
                        try {
                            JSONObject errorJson = new JSONObject(responseData);
                            String errorMessage = errorJson.optString("error_description", 
                                    errorJson.optString("error", "Login failed"));
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void upsertProfile(String userId, String email, String fullName, String accessToken) {
        String profileUrl = BuildConfig.SUPABASE_URL + "/rest/v1/profiles";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id", userId);
            jsonBody.put("full_name", fullName);
            jsonBody.put("email", email);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error creating profile upsert body", e);
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to upsert profile: " + e.getMessage());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String resStr = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Profile upsert response: " + response.code() + ", " + resStr);
                } catch (Exception ex) {}
            }
        });
    }

    private void handleLoginSuccess(String userId, String name, String token, String refreshToken) {
        runOnUiThread(() -> {
            btnSignIn.setEnabled(true);
            SessionManager session = new SessionManager(LoginActivity.this);
            session.setUserId(userId);
            session.setUserName(name);
            session.setAccessToken(token);
            session.setRefreshToken(refreshToken);

            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
