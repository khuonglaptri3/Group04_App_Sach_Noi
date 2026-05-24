package com.example.a23110035_23110060;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

public class ProfileFragment extends Fragment {

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

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
                com.google.android.material.button.MaterialButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                btnPlay.setIconResource(PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            } else {
                miniPlayer.setVisibility(View.GONE);
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