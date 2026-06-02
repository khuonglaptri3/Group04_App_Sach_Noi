package com.example.a23110035_23110060.controller;

import com.example.a23110035_23110060.BuildConfig;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import java.io.IOException;

public class NetworkClient {
    private static OkHttpClient client;

    public static synchronized OkHttpClient getClient(Context context) {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .authenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, @NonNull Response response) throws IOException {
                            // This is called when the server returns 401 Unauthorized
                            Log.d("NetworkClient", "401 Unauthorized detected, attempting token refresh...");
                            
                            SessionManager session = new SessionManager(context.getApplicationContext());
                            String refreshToken = session.getRefreshToken();
                            
                            if (refreshToken == null) return null;

                            // Sync refresh (blocking call)
                            String newToken = syncRefresh(context.getApplicationContext(), refreshToken);
                            
                            if (newToken != null) {
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newToken)
                                        .build();
                            }
                            
                            return null;
                        }
                    })
                    .build();
        }
        return client;
    }

    private static String syncRefresh(Context context, String refreshToken) {
        OkHttpClient refreshClient = new OkHttpClient();
        String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token";
        
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("refresh_token", refreshToken);
        } catch (Exception e) {}

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                json.toString(), 
                okhttp3.MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .post(body)
                .build();

        try (Response response = refreshClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.body().string());
                String newAccessToken = jsonResponse.getString("access_token");
                String newRefreshToken = jsonResponse.getString("refresh_token");

                SessionManager session = new SessionManager(context);
                session.setAccessToken(newAccessToken);
                session.setRefreshToken(newRefreshToken);
                
                Log.d("NetworkClient", "Token refreshed successfully");
                return newAccessToken;
            }
        } catch (Exception e) {
            Log.e("NetworkClient", "Refresh failed", e);
        }
        return null;
    }
}
