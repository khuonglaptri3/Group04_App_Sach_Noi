package com.example.a23110035_23110060.view.fragment;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.LoginActivity;
import com.example.a23110035_23110060.view.activity.EditProfileActivity;
import com.example.a23110035_23110060.view.activity.PremiumActivity;
import com.example.a23110035_23110060.view.activity.DownloadManagerActivity;
import com.example.a23110035_23110060.view.activity.SecurityPrivacyActivity;

import com.example.a23110035_23110060.controller.SessionManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileContentFragment extends Fragment {

    private SessionManager sessionManager;
    private OkHttpClient client = new OkHttpClient();
    private TextView tvProfileName, tvBooksReadCount, tvListeningHours, tvBadgesCount;
    private View cardPremiumUpgrade;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvBooksReadCount = view.findViewById(R.id.tvBooksReadCount);
        tvListeningHours = view.findViewById(R.id.tvListeningHours);
        tvBadgesCount = view.findViewById(R.id.tvBadgesCount);
        cardPremiumUpgrade = view.findViewById(R.id.cardPremiumUpgrade);

        // Bind user name from SessionManager
        if (tvProfileName != null) {
            tvProfileName.setText(sessionManager.getUserName());
        }

        // Fetch user statistics from Supabase RPC
        fetchUserSummary();
        checkUserPremiumStatus();

        // Bind Upgrade Button
        View btnUpgrade = view.findViewById(R.id.btnUpgradeProfile);
        if (btnUpgrade != null) {
            btnUpgrade.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), PremiumActivity.class));
            });
        }

        // Bind Logout Button
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clear();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }

        View llEditProfile = view.findViewById(R.id.ll_edit_profile);
        if (llEditProfile != null) {
            llEditProfile.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
            });
        }

        View llSecurity = view.findViewById(R.id.ll_security);
        if (llSecurity != null) {
            llSecurity.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), SecurityPrivacyActivity.class));
            });
        }

        View btnUpgradeProfile = view.findViewById(R.id.btnUpgradeProfile);
        if (btnUpgradeProfile != null) {
            btnUpgradeProfile.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), PremiumActivity.class));
            });
        }
        
        View llDownloads = view.findViewById(R.id.ll_downloads);
        if (llDownloads != null) {
            llDownloads.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), DownloadManagerActivity.class));
            });
        }

        fetchProfileDetails();
    }

    private void fetchProfileDetails() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=is_premium,avatar_url";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        org.json.JSONArray array = new org.json.JSONArray(response.body().string());
                        if (array.length() > 0) {
                            JSONObject profile = array.getJSONObject(0);
                            boolean isPremium = profile.optBoolean("is_premium", false);
                            String avatarUrl = profile.optString("avatar_url", null);
                            
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    View cvPremiumBanner = getView() != null ? getView().findViewById(R.id.cardPremiumUpgrade) : null;
                                    if (cvPremiumBanner != null) {
                                        if (isPremium) {
                                            cvPremiumBanner.setVisibility(View.GONE);
                                        } else {
                                            cvPremiumBanner.setVisibility(View.VISIBLE);
                                        }
                                    }
                                    
                                    ImageView ivAvatar = getView() != null ? getView().findViewById(R.id.ivAvatar) : null;
                                    if (ivAvatar != null && avatarUrl != null && !avatarUrl.isEmpty()) {
                                        com.bumptech.glide.Glide.with(ProfileContentFragment.this)
                                                .load(avatarUrl)
                                                .placeholder(R.drawable.bacl)
                                                .into(ivAvatar);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ProfileContent", "Parse profile error", e);
                    }
                }
            }
        });
    }

    private void fetchUserSummary() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/get_user_reading_summary";
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("target_user_id", userId);
        } catch (Exception e) {
            return;
        }

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("ProfileContent", "Failed to fetch summary", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject result = new JSONObject(response.body().string());
                        int booksReadCount = result.optInt("books_read_count", 0);
                        double listeningHours = result.optDouble("listening_hours", 0.0);
                        int totalBadges = result.optInt("total_badges_unlocked", 0);

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (tvBooksReadCount != null) {
                                    tvBooksReadCount.setText(String.valueOf(booksReadCount));
                                }
                                if (tvListeningHours != null) {
                                    tvListeningHours.setText(String.format(Locale.getDefault(), "%.0f", listeningHours));
                                }
                                if (tvBadgesCount != null) {
                                    tvBadgesCount.setText(String.valueOf(totalBadges));
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ProfileContent", "Error parsing summary", e);
                    }
                }
            }
        });
    }

    private void checkUserPremiumStatus() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=is_premium";
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
                            boolean isPremium = array.getJSONObject(0).optBoolean("is_premium", false);
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    if (cardPremiumUpgrade != null) {
                                        cardPremiumUpgrade.setVisibility(isPremium ? View.GONE : View.VISIBLE);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }
}