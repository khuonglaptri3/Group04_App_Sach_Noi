package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.controller.SessionManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SecurityPrivacyActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword;
    private MaterialButton btnChangePassword;
    private MaterialButton btnDeleteAccount;
    
    private SessionManager sessionManager;
    private OkHttpClient client = new OkHttpClient();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_privacy);

        sessionManager = new SessionManager(this);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNewPassword = findViewById(R.id.et_new_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);

        btnChangePassword.setOnClickListener(v -> changePassword());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void changePassword() {
        String newPassword = etNewPassword.getText().toString();
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getAccessToken();
        if (token == null) return;

        progressDialog.show();
        String url = BuildConfig.SUPABASE_URL + "/auth/v1/user";
        JSONObject json = new JSONObject();
        try {
            json.put("password", newPassword);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SecurityPrivacyActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(SecurityPrivacyActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                        etNewPassword.setText("");
                    } else {
                        Toast.makeText(SecurityPrivacyActivity.this, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản của mình? Mọi dữ liệu sẽ bị xóa và không thể khôi phục.")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> deleteAccount())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteAccount() {
        String token = sessionManager.getAccessToken();
        if (token == null) return;

        progressDialog.show();
        // Gọi RPC function để xóa tài khoản
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/delete_user_account";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SecurityPrivacyActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(SecurityPrivacyActivity.this, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show();
                        sessionManager.clear();
                        Intent intent = new Intent(SecurityPrivacyActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SecurityPrivacyActivity.this, "Lỗi xóa tài khoản (RPC chưa cấu hình?)", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
