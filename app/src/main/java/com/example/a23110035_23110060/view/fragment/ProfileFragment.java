package com.example.a23110035_23110060.view.fragment;

import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.AudioPlayerActivity;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.controller.PlayerManager;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

public class ProfileFragment extends Fragment {

    private final Handler progressHandler = new Handler();
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

        View greetingLayout = view.findViewById(R.id.tvGreetingSub).getParent() instanceof View 
                ? (View) view.findViewById(R.id.tvGreetingSub).getParent() : null;
        if (greetingLayout != null) greetingLayout.setVisibility(View.GONE);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) tabLayout.setVisibility(View.GONE);

        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new ProfileContentFragment();
            }
            @Override
            public int getItemCount() { return 1; }
        });

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

                android.widget.ImageButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                if (btnPlay != null) {
                    btnPlay.setImageResource(PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                }
                boolean isPlaying = PlayerManager.getInstance().isPlaying();
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
