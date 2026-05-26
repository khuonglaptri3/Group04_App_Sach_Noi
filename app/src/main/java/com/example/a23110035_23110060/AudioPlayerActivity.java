package com.example.a23110035_23110060;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class  AudioPlayerActivity extends AppCompatActivity {

    private ImageButton btnCollapse, btnSkipPrevious, btnSkipNext, btnReplay10, btnForward30;
    private FloatingActionButton fabPlay;
    private SeekBar seekBar;
    private TextView tvTitle, tvAuthor, tvCurrentTime, tvTotalTime, tvSpeedValue;
    private ImageView ivArtwork;
    
    private PlayerManager playerManager;
    private float[] speeds = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private int speedIndex = 1; // 1.0x

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn_collapse), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top + (int)(16 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(mlp);
            return windowInsets;
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn_bookmark), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top + (int)(16 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(mlp);
            return windowInsets;
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ll_secondary_controls), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom + (int)(32 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(mlp);
            return windowInsets;
        });

        playerManager = PlayerManager.getInstance();
        initViews();
        setupListeners();
        updateUI();

        playerManager.setCallback(new PlayerManager.PlayerCallback() {
            @Override
            public void onProgress(int currentMs, int totalMs) {
                runOnUiThread(() -> {
                    seekBar.setMax(totalMs);
                    seekBar.setProgress(currentMs);
                    tvCurrentTime.setText(formatTime(currentMs));
                    tvTotalTime.setText("-" + formatTime(totalMs - currentMs));
                });
            }

            @Override
            public void onStateChange(boolean isPlaying) {
                runOnUiThread(() -> {
                    fabPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                });
            }
        });
    }

    private void initViews() {
        btnCollapse = findViewById(R.id.btn_collapse);
        btnSkipPrevious = findViewById(R.id.btn_skip_previous);
        btnSkipNext = findViewById(R.id.btn_skip_next);
        btnReplay10 = findViewById(R.id.btn_replay_10);
        btnForward30 = findViewById(R.id.btn_forward_30);
        fabPlay = findViewById(R.id.fab_play);
        seekBar = findViewById(R.id.seek_bar);
        tvTitle = findViewById(R.id.tv_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        ivArtwork = findViewById(R.id.iv_artwork);
        tvSpeedValue = findViewById(R.id.tv_speed_value);
    }

    private void setupListeners() {
        btnCollapse.setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> saveAudioBookmark());
        fabPlay.setOnClickListener(v -> playerManager.togglePlayPause());
        btnReplay10.setOnClickListener(v -> playerManager.seekBack(10000));
        btnForward30.setOnClickListener(v -> playerManager.seekForward(30000));
        btnSkipPrevious.setOnClickListener(v -> playerManager.previousChapter());
        btnSkipNext.setOnClickListener(v -> playerManager.nextChapter());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) playerManager.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.ll_speed).setOnClickListener(v -> {
            float currentSpeed = speeds[speedIndex];
            AudioSettingsBottomSheet bottomSheet = new AudioSettingsBottomSheet(new AudioSettingsBottomSheet.AudioSettingsListener() {
                @Override
                public void onSpeedChanged(float speed) {
                    playerManager.setPlaybackSpeed(speed);
                    tvSpeedValue.setText(String.format(Locale.getDefault(), "%.2fx", speed));
                    // Update index if it matches our presets
                    for (int i = 0; i < speeds.length; i++) {
                        if (Math.abs(speeds[i] - speed) < 0.01f) {
                            speedIndex = i;
                            break;
                        }
                    }
                }

                @Override
                public void onTimerSet(int minutes) {
                    if (minutes > 0) {
                        playerManager.setSleepTimer(minutes);
                    } else {
                        playerManager.cancelSleepTimer();
                    }
                }
            }, currentSpeed);
            bottomSheet.show(getSupportFragmentManager(), "AudioSettings");
        });

        findViewById(R.id.ll_timer).setOnClickListener(v -> {
            findViewById(R.id.ll_speed).performClick(); // Same bottom sheet
        });

        findViewById(R.id.ll_chapters).setOnClickListener(v -> {
            Book book = playerManager.getCurrentBook();
            if (playerManager.getChapters() != null && !playerManager.getChapters().isEmpty()) {
                java.util.List<String> titles = new java.util.ArrayList<>();
                for (Chapter c : playerManager.getChapters()) {
                    titles.add(c.getTitle() != null ? c.getTitle() : "Chương " + c.getChapterNumber());
                }
                EbookChaptersBottomSheet bottomSheet = new EbookChaptersBottomSheet(titles, index -> {
                    playerManager.playChapter(index);
                });
                
                // Set the current playing chapter index to highlight it
                int currentIdx = -1;
                long currentPos = playerManager.getCurrentPosition();
                for (int i = 0; i < playerManager.getChapters().size(); i++) {
                    Chapter c = playerManager.getChapters().get(i);
                    if (currentPos >= c.getStartTime() && (c.getEndTime() == 0 || currentPos < c.getEndTime())) {
                        currentIdx = i;
                        break;
                    }
                }
                bottomSheet.setCurrentChapterIndex(currentIdx);
                
                bottomSheet.show(getSupportFragmentManager(), "AudioChapters");
            } else {
                Toast.makeText(this, "Không có mục lục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(); // Cập nhật lại mỗi khi quay lại màn hình
    }

    private void updateUI() {
        Book book = playerManager.getCurrentBook();
        if (book != null) {
            tvTitle.setText(book.getTitle());
            tvAuthor.setText(book.getAuthorName());
            
            if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                Glide.with(this)
                    .load(book.getCoverUrl())
                    .placeholder(R.drawable.bacl)
                    .error(R.drawable.bacl)
                    .into(ivArtwork);
            }
        } else {
            tvTitle.setText("Chưa có sách nào");
            tvAuthor.setText("-");
        }
        fabPlay.setImageResource(playerManager.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void saveAudioBookmark() {
        Book book = playerManager.getCurrentBook();
        if (book == null) return;
        
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        int positionSeconds = playerManager.getCurrentPosition() / 1000;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks";
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", book.getId());
            json.put("position_seconds", positionSeconds);
            json.put("note", "Đánh dấu tại " + formatTime(playerManager.getCurrentPosition()));
        } catch (Exception e) {}

        okhttp3.RequestBody body = okhttp3.RequestBody.create(json.toString(), okhttp3.MediaType.parse("application/json"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull java.io.IOException e) {}
            @Override
            public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull okhttp3.Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(AudioPlayerActivity.this, "Đã đánh dấu âm thanh", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}