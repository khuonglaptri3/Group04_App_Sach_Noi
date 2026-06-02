package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.controller.SessionManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextInputEditText etFullName;
    private MaterialButton btnSave;
    
    private SessionManager sessionManager;
    private final OkHttpClient client = new OkHttpClient();
    
    private Uri selectedImageUri;
    private String currentAvatarUrl;
    
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).into(ivAvatar);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        ivAvatar = findViewById(R.id.iv_avatar);
        etFullName = findViewById(R.id.et_full_name);
        btnSave = findViewById(R.id.btn_save);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang lưu...");
        progressDialog.setCancelable(false);

        ivAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveProfile());

        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=*";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        org.json.JSONArray array = new org.json.JSONArray(response.body().string());
                        if (array.length() > 0) {
                            JSONObject profile = array.getJSONObject(0);
                            String fullName = profile.optString("full_name", "");
                            currentAvatarUrl = profile.optString("avatar_url", "");
                            
                            runOnUiThread(() -> {
                                etFullName.setText(fullName);
                                if (!currentAvatarUrl.isEmpty() && !currentAvatarUrl.equals("null")) {
                                    Glide.with(EditProfileActivity.this).load(currentAvatarUrl).placeholder(R.drawable.bacl).into(ivAvatar);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("EditProfile", "Parse error", e);
                    }
                }
            }
        });
    }

    private void saveProfile() {
        String newName = etFullName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        if (selectedImageUri != null) {
            uploadAvatarAndSave(newName);
        } else {
            updateProfileInDb(newName, currentAvatarUrl);
        }
    }

    private void uploadAvatarAndSave(String newName) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();

                String filename = sessionManager.getUserId() + "_" + UUID.randomUUID().toString() + ".jpg";
                String url = BuildConfig.SUPABASE_URL + "/storage/v1/object/avatars/" + filename;

                RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String publicUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + filename;
                    updateProfileInDb(newName, publicUrl);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e("EditProfile", "Upload failed: " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            progressDialog.dismiss();
                            new android.app.AlertDialog.Builder(EditProfileActivity.this)
                                .setTitle("Lỗi Upload Supabase (" + response.code() + ")")
                                .setMessage(errorBody)
                                .setPositiveButton("OK", null)
                                .show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("EditProfile", "Upload error", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.dismiss();
                        new android.app.AlertDialog.Builder(EditProfileActivity.this)
                            .setTitle("Lỗi đọc ảnh")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    }
                });
            }
        }).start();
    }

    private void updateProfileInDb(String newName, String avatarUrl) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;
        JSONObject json = new JSONObject();
        try {
            json.put("full_name", newName);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                json.put("avatar_url", avatarUrl);
            }
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.dismiss();
                        if (response.isSuccessful()) {
                            sessionManager.setUserName(newName);
                            Toast.makeText(EditProfileActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
