package com.example.a23110035_23110060;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import org.json.JSONArray;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private String[] titles = new String[]{"Sách nói", "Sách điện tử"};
    private SessionManager sessionManager;
    private TextView tvGreeting;
    private OkHttpClient client = new OkHttpClient();
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private ObjectAnimator rotateAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        tvGreeting = view.findViewById(R.id.tvGreetingSub);
        
        updateGreeting(sessionManager.getUserName());
        fetchUserProfile();

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        viewPager.setAdapter(new HomePagerAdapter(this, titles));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(titles[position]);
        }).attach();

        setupMiniPlayer(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncMiniPlayer();
        startProgressUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopProgressUpdate();
    }

    private void syncMiniPlayer() {
        View miniPlayer = getView() != null ? getView().findViewById(R.id.miniPlayer) : null;
        if (miniPlayer != null) {
            Book currentBook = PlayerManager.getInstance().getCurrentBook();
            if (currentBook != null) {
                miniPlayer.setVisibility(View.VISIBLE);
                ((TextView) miniPlayer.findViewById(R.id.tvMiniTitle)).setText(currentBook.getTitle());
                ((TextView) miniPlayer.findViewById(R.id.tvMiniAuthor)).setText(currentBook.getAuthorName());
                
                ImageView ivCover = miniPlayer.findViewById(R.id.ivMiniCover);
                Glide.with(this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivCover);

                com.google.android.material.button.MaterialButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                boolean isPlaying = PlayerManager.getInstance().isPlaying();
                btnPlay.setIconResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

                handleRotation(ivCover, isPlaying);
            } else {
                miniPlayer.setVisibility(View.GONE);
            }
        }
    }

    private void handleRotation(View view, boolean isPlaying) {
        if (rotateAnimator == null) {
            rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
            rotateAnimator.setDuration(10000);
            rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnimator.setInterpolator(new LinearInterpolator());
            rotateAnimator.start();
        }
        if (isPlaying) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (rotateAnimator.isPaused()) rotateAnimator.resume();
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                rotateAnimator.pause();
            }
        }
    }

    private void startProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                View miniPlayer = getView() != null ? getView().findViewById(R.id.miniPlayer) : null;
                if (miniPlayer != null && PlayerManager.getInstance().isPlaying()) {
                    LinearProgressIndicator progressIndicator = miniPlayer.findViewById(R.id.miniPlayerProgress);
                    int current = PlayerManager.getInstance().getCurrentPosition();
                    int total = PlayerManager.getInstance().getDuration();
                    if (total > 0) {
                        progressIndicator.setMax(total);
                        progressIndicator.setProgress(current);
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.postDelayed(progressRunnable, 1000);
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
    }

    private void updateGreeting(String name) {
        if (tvGreeting != null) {
            tvGreeting.setText("Xin chào, " + name + " 👋");
        }
    }

    private void fetchUserProfile() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=full_name";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            String fullName = array.getJSONObject(0).getString("full_name");
                            sessionManager.setUserName(fullName);
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> updateGreeting(fullName));
                            }
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Profile fetch error", e);
                    }
                }
            }
        });
    }

    private void setupMiniPlayer(View view) {
        View miniPlayer = view.findViewById(R.id.miniPlayer);
        if (miniPlayer != null) {
            miniPlayer.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AudioPlayerActivity.class);
                startActivity(intent);
            });
            miniPlayer.findViewById(R.id.btnMiniPlayPause).setOnClickListener(v -> {
                PlayerManager.getInstance().togglePlayPause();
                syncMiniPlayer();
            });
            miniPlayer.findViewById(R.id.btnMiniNext).setOnClickListener(v -> {
                PlayerManager.getInstance().skipNext();
                syncMiniPlayer();
            });
        }
    }
}
