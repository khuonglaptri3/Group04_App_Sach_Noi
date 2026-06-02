package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SUPABASE_SIGNUP";
    private EditText etFullName, etEmailSignUp, etPasswordSignUp, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLoginLink;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signUpMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        etFullName = findViewById(R.id.etFullName);
        etEmailSignUp = findViewById(R.id.etEmailSignUp);
        etPasswordSignUp = findViewById(R.id.etPasswordSignUp);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        client = new OkHttpClient();

        // Already have account? Login link
        tvLoginLink.setOnClickListener(v -> finish());

        // Sign Up Button Click
        btnSignUp.setOnClickListener(v -> performSignUp());
    }

    private void performSignUp() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmailSignUp.getText().toString().trim();
        String password = etPasswordSignUp.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Validation
        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmailSignUp.setError("Email is required");
            etEmailSignUp.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailSignUp.setError("Please enter a valid email address");
            etEmailSignUp.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPasswordSignUp.setError("Password must be at least 6 characters");
            etPasswordSignUp.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // 2. Call Supabase API
        signUpWithSupabase(fullName, email, password);
    }

    private void signUpWithSupabase(String fullName, String email, String password) {
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY;

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseAnonKey == null || supabaseAnonKey.isEmpty()) {
            Toast.makeText(this, "Supabase configuration missing", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SUPABASE_URL or SUPABASE_ANON_KEY is empty in BuildConfig");
            return;
        }

        String url = supabaseUrl + "/auth/v1/signup";

        // Construct JSON Body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            
            JSONObject data = new JSONObject();
            data.put("full_name", fullName);
            jsonBody.put("data", data);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Construction Error", e);
            Toast.makeText(this, "Error preparing data", Toast.LENGTH_SHORT).show();
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

        btnSignUp.setEnabled(false); // Disable button during request

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network Error: " + e.getMessage());
                runOnUiThread(() -> {
                    btnSignUp.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Body: " + responseData);

                runOnUiThread(() -> {
                    btnSignUp.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Sign up successful! Please check your email for confirmation.", Toast.LENGTH_LONG).show();
                        finish(); // Return to LoginActivity
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(responseData);
                            String errorMessage = errorJson.optString("msg", "Sign up failed");
                            Toast.makeText(SignUpActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Supabase Error: " + responseData);
                        } catch (JSONException e) {
                            Toast.makeText(SignUpActivity.this, "An unexpected error occurred: " + response.code(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "JSON Parsing Error", e);
                        }
                    }
                });
            }
        });
    }
}
