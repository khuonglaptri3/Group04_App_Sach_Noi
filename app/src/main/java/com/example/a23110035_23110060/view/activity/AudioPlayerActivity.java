package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.model.Chapter;
import com.example.a23110035_23110060.controller.PlayerManager;
import com.example.a23110035_23110060.controller.SessionManager;

import android.os.Bundle;
import android.view.View;
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

    private ImageButton btnCollapse, btnSkipPrevious, btnSkipNext, btnReplay10, btnForward10;
    private FloatingActionButton fabPlay;
    private SeekBar seekBar;
    private TextView tvTitle, tvAuthor, tvCurrentTime, tvTotalTime, tvSpeedValue;
    private ImageView ivArtwork;
    
    private PlayerManager playerManager;
    private final float[] speeds = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private int speedIndex = 1; // 1.0x

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, insets.top, 0, insets.bottom);
            return windowInsets;
        });

        playerManager = PlayerManager.getInstance();
        initViews();
        setupListeners();
        updateUI();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        playerManager.setCallback(new PlayerManager.PlayerCallback() {
            @Override
            public void onProgress(int currentMs, int totalMs) {
                runOnUiThread(() -> {
                    seekBar.setMax(totalMs);
                    seekBar.setProgress(currentMs);
                    tvCurrentTime.setText(formatTime(currentMs));
                    tvTotalTime.setText("-" + formatTime(totalMs - currentMs));
                    
                    TextView tvHeader = findViewById(R.id.tv_header);
                    if (tvHeader != null) {
                        String title = playerManager.getCurrentChapterTitle();
                        if (title != null) tvHeader.setText(title.toUpperCase());
                    }
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
        btnForward10 = findViewById(R.id.btn_forward_10);
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
        
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> {
            Book book = playerManager.getCurrentBook();
            if (book != null) {
                int positionSeconds = playerManager.getCurrentPosition() / 1000;
                com.example.a23110035_23110060.view.bottomsheet.AudioBookmarkAddFragment.newInstance(book.getId(), positionSeconds)
                    .show(getSupportFragmentManager(), "AudioBookmarkAdd");
            }
        });
        
        View btnBookmarks = findViewById(R.id.ll_bookmarks);
        if (btnBookmarks != null) {
            btnBookmarks.setOnClickListener(v -> {
                Book book = playerManager.getCurrentBook();
                if (book != null) {
                    com.example.a23110035_23110060.view.bottomsheet.AudioBookmarkListFragment.newInstance(book.getId())
                        .show(getSupportFragmentManager(), "AudioBookmarkList");
                }
            });
        }
        

        
        fabPlay.setOnClickListener(v -> playerManager.togglePlayPause());
        btnReplay10.setOnClickListener(v -> playerManager.seekBack(10000));
        btnForward10.setOnClickListener(v -> playerManager.seekForward(10000));
        btnSkipPrevious.setOnClickListener(v -> playerManager.previousChapter());
        btnSkipNext.setOnClickListener(v -> playerManager.nextChapter());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                    tvTotalTime.setText("-" + formatTime(seekBar.getMax() - progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playerManager.seekTo(seekBar.getProgress());
                TextView tvHeader = findViewById(R.id.tv_header);
                if (tvHeader != null) {
                    String title = playerManager.getCurrentChapterTitle();
                    if (title != null) tvHeader.setText(title.toUpperCase());
                }
            }
        });

        findViewById(R.id.ll_speed).setOnClickListener(v -> {
            float currentSpeed = speeds[speedIndex];
            com.example.a23110035_23110060.view.bottomsheet.SpeedBottomSheetFragment bottomSheet = 
                new com.example.a23110035_23110060.view.bottomsheet.SpeedBottomSheetFragment(currentSpeed, new com.example.a23110035_23110060.view.bottomsheet.SpeedBottomSheetFragment.SpeedListener() {
                    @Override
                    public void onSpeedSelected(float speed) {
                        playerManager.setPlaybackSpeed(speed);
                        tvSpeedValue.setText(String.format(Locale.getDefault(), "%.2fx", speed));
                        for (int i = 0; i < speeds.length; i++) {
                            if (Math.abs(speeds[i] - speed) < 0.01f) {
                                speedIndex = i;
                                break;
                            }
                        }
                    }
                });
            bottomSheet.show(getSupportFragmentManager(), "SpeedSettings");
        });

        findViewById(R.id.ll_timer).setOnClickListener(v -> {
            com.example.a23110035_23110060.view.bottomsheet.TimerBottomSheetFragment bottomSheet = 
                new com.example.a23110035_23110060.view.bottomsheet.TimerBottomSheetFragment(new com.example.a23110035_23110060.view.bottomsheet.TimerBottomSheetFragment.TimerListener() {
                    @Override
                    public void onTimerSelected(int minutes) {
                        if (minutes > 0) {
                            playerManager.setSleepTimer(minutes);
                            Toast.makeText(AudioPlayerActivity.this, "Sẽ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
                        } else {
                            playerManager.cancelSleepTimer();
                            Toast.makeText(AudioPlayerActivity.this, "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            bottomSheet.show(getSupportFragmentManager(), "TimerSettings");
        });

        findViewById(R.id.ll_chapters).setOnClickListener(v -> {
            Book book = playerManager.getCurrentBook();
            if (playerManager.getChapters() != null && !playerManager.getChapters().isEmpty()) {
                java.util.List<String> titles = new java.util.ArrayList<>();
                for (Chapter c : playerManager.getChapters()) {
                    titles.add(c.getTitle() != null ? c.getTitle() : "Chương " + c.getChapterNumber());
                }
//                EbookChaptersBottomSheet bottomSheet = new EbookChaptersBottomSheet(titles, index -> {
//                    playerManager.playChapter(index);
//                });
                EbookChaptersBottomSheet bottomSheet = new EbookChaptersBottomSheet(titles, new EbookChaptersBottomSheet.ChapterListener() {
                    @Override
                    public void onChapterSelected(int index) {
                        playerManager.playChapter(index);
                    }
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
            
            TextView tvHeader = findViewById(R.id.tv_header);
            if (tvHeader != null) {
                String chapterTitle = playerManager.getCurrentChapterTitle();
                if (chapterTitle != null) tvHeader.setText(chapterTitle.toUpperCase());
            }

            // Sync progress immediately on resume or update
            try {
                int current = playerManager.getCurrentPosition();
                int total = playerManager.getDuration();
                if (total > 0) {
                    seekBar.setMax(total);
                    seekBar.setProgress(current);
                    tvCurrentTime.setText(formatTime(current));
                    tvTotalTime.setText("-" + formatTime(total - current));
                } else {
                    seekBar.setProgress(0);
                    tvCurrentTime.setText("00:00");
                    tvTotalTime.setText("00:00");
                }
            } catch (Exception e) {
                seekBar.setProgress(0);
                tvCurrentTime.setText("00:00");
                tvTotalTime.setText("00:00");
            }
        } else {
            tvTitle.setText("Chưa có sách nào");
            tvAuthor.setText("-");
            seekBar.setProgress(0);
            tvCurrentTime.setText("00:00");
            tvTotalTime.setText("00:00");
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

        com.example.a23110035_23110060.controller.NetworkClient.getClient(this).newCall(request).enqueue(new okhttp3.Callback() {
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