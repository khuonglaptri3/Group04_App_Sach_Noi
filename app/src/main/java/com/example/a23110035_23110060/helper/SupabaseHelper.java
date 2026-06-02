package com.example.a23110035_23110060.helper;

import com.example.a23110035_23110060.view.activity.LoginActivity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.controller.SessionManager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseHelper {
    private static final String TAG = "SupabaseHelper";
    private static final OkHttpClient client = new OkHttpClient();

    public interface RefreshCallback {
        void onSuccess(String newToken);
        void onError(String error);
    }

    public static void refreshAccessToken(Context context, RefreshCallback callback) {
        SessionManager session = new SessionManager(context);
        String refreshToken = session.getRefreshToken();

        if (refreshToken == null) {
            logout(context);
            if (callback != null) callback.onError("No refresh token");
            return;
        }

        String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token";
        JSONObject json = new JSONObject();
        try {
            json.put("refresh_token", refreshToken);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        String newAccessToken = jsonResponse.getString("access_token");
                        String newRefreshToken = jsonResponse.getString("refresh_token");

                        session.setAccessToken(newAccessToken);
                        session.setRefreshToken(newRefreshToken);

                        if (callback != null) callback.onSuccess(newAccessToken);
                    } catch (Exception e) {
                        if (callback != null) callback.onError("Parse error");
                    }
                } else {
                    // Refresh token might be expired too
                    logout(context);
                    if (callback != null) callback.onError("Session expired");
                }
            }
        });
    }

    public static void logout(Context context) {
        SessionManager session = new SessionManager(context);
        session.clear();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
