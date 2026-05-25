package com.example.a23110035_23110060;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
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

public class EbookReaderActivity extends AppCompatActivity {

    private String bookId;
    private SessionManager sessionManager;
    private OkHttpClient client = new OkHttpClient();

    private View settingsPanel, selectionPopup, brightnessOverlay, scrollContent;
    private LinearLayout bottomContainer;
    private TextView tvFontSizeDisplay, tvToolbarTitle, tvToolbarChapter, tvContentChapterTitle, tvReaderBody;
    private float currentTextSize = 18;
    private boolean isNightMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_reader);

        sessionManager = new SessionManager(this);
        bookId = getIntent().getStringExtra("bookId");

        initViews();
        setupListeners();
        fetchBookContent();
    }

    private void initViews() {
        bottomContainer = findViewById(R.id.bottom_container);
        settingsPanel = findViewById(R.id.settings_panel);
        selectionPopup = findViewById(R.id.selection_popup);
        brightnessOverlay = findViewById(R.id.view_brightness_overlay);
        scrollContent = findViewById(R.id.scroll_content);
        tvFontSizeDisplay = findViewById(R.id.tv_font_size);
        
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvToolbarChapter = findViewById(R.id.tv_toolbar_chapter);
        tvContentChapterTitle = findViewById(R.id.tv_content_chapter_title);
        tvReaderBody = findViewById(R.id.tv_reader_body);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> addBookmark());
        
        findViewById(R.id.btn_search).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tìm kiếm")
                .setMessage("Tính năng tìm kiếm sẽ được cập nhật sớm.")
                .setPositiveButton("Đóng", null)
                .show();
        });
        
        findViewById(R.id.btn_contents).setOnClickListener(v -> {
            String[] mockChapters = {"Chương 1: Bắt đầu", "Chương 2: Diễn biến", "Chương 3: Kết thúc"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mục lục")
                .setItems(mockChapters, (dialog, which) -> {
                    Toast.makeText(this, "Chuyển đến " + mockChapters[which], Toast.LENGTH_SHORT).show();
                    // TODO: scroll to chapter
                })
                .show();
        });
        
        View btnMore = findViewById(R.id.btn_more);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
                popup.getMenu().add("Chia sẻ");
                popup.getMenu().add("Báo lỗi");
                popup.setOnMenuItemClickListener(item -> {
                    Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        findViewById(R.id.btn_night_mode).setOnClickListener(v -> {
            isNightMode = !isNightMode;
            if (isNightMode) applyTheme("#1A1A2E", "#E0E0E0");
            else applyTheme("#FFFFFF", "#1D1B20");
        });

        findViewById(R.id.btn_settings).setOnClickListener(v -> toggleSettingsPanel());

        findViewById(R.id.btn_notes).setOnClickListener(v -> {
            BookNotesFragment fragment = BookNotesFragment.newInstance(bookId);
            fragment.show(getSupportFragmentManager(), "BookNotesFragment");
        });

        setupSettingsPanel();
        setupSelectionPopup();
        setupScrollAndSwipe();
    }

    private void setupScrollAndSwipe() {
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scroll_content);
        
        scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            int maxScroll = v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight();
            if (maxScroll > 0) {
                int percentage = (int) (((float) scrollY / maxScroll) * 100);
                TextView tvPercent = findViewById(R.id.tv_percent_read);
                com.google.android.material.progressindicator.LinearProgressIndicator pbReading = findViewById(R.id.pb_reading);
                
                tvPercent.setText(percentage + "% đã đọc");
                pbReading.setProgress(percentage);
                
                TextView tvPage = findViewById(R.id.tv_page_info);
                int pageNum = (scrollY / (v.getHeight() > 0 ? v.getHeight() : 1)) + 1;
                tvPage.setText("Trang " + pageNum);
            }
        });

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null && Math.abs(velocityX) > Math.abs(velocityY)) {
                    int pageHeight = scrollView.getHeight() - 50;
                    if (velocityX < 0) { // Swipe Left -> Next Page
                        scrollView.smoothScrollBy(0, pageHeight);
                        return true;
                    } else if (velocityX > 0) { // Swipe Right -> Prev Page
                        scrollView.smoothScrollBy(0, -pageHeight);
                        return true;
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        scrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Let scrollview handle normal scrolls too
        });
    }

    private void fetchBookContent() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/chapters?book_id=eq." + bookId + "&order=chapter_number.asc";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchLegacyDescription();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            StringBuilder fullContent = new StringBuilder();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                String title = obj.optString("title", "Chương " + (i + 1));
                                String text = obj.optString("text_content", "");
                                fullContent.append(title).append("\n\n").append(text).append("\n\n---\n\n");
                            }
                            String firstChapterTitle = array.getJSONObject(0).optString("title", "Chương 1");
                            runOnUiThread(() -> {
                                tvContentChapterTitle.setText(firstChapterTitle);
                                tvReaderBody.setText(fullContent.toString());
                            });
                        } else {
                            fetchLegacyDescription();
                        }
                    } catch (Exception e) {
                        fetchLegacyDescription();
                    }
                } else {
                    fetchLegacyDescription();
                }
            }
        });
    }

    private void fetchLegacyDescription() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?id=eq." + bookId + "&select=title,description";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EbookReader", "Fetch content failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            JSONObject obj = array.getJSONObject(0);
                            String title = obj.getString("title");
                            String content = obj.optString("description", "Không có nội dung cho sách này.");

                            runOnUiThread(() -> {
                                tvToolbarTitle.setText(title);
                                tvToolbarChapter.setText("Chương 1");
                                tvContentChapterTitle.setText("Giới thiệu");
                                tvReaderBody.setText(content);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("EbookReader", "Parse content error", e);
                    }
                }
            }
        });
    }

    private void setupSettingsPanel() {
        SeekBar seekBrightness = findViewById(R.id.seek_brightness);
        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = (100 - progress) / 100f * 0.8f;
                brightnessOverlay.setVisibility(alpha > 0 ? View.VISIBLE : View.GONE);
                brightnessOverlay.setAlpha(alpha);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btn_font_decrease).setOnClickListener(v -> adjustFontSize(-2));
        findViewById(R.id.btn_font_increase).setOnClickListener(v -> adjustFontSize(2));
        findViewById(R.id.btn_theme_light).setOnClickListener(v -> applyTheme("#FFFFFF", "#1D1B20"));
        findViewById(R.id.btn_theme_sepia).setOnClickListener(v -> applyTheme("#F5E6C8", "#5D4037"));
        findViewById(R.id.btn_theme_dark).setOnClickListener(v -> applyTheme("#1A1A2E", "#E0E0E0"));
    }

    private void adjustFontSize(int delta) {
        currentTextSize = Math.max(14, Math.min(28, currentTextSize + delta));
        tvFontSizeDisplay.setText(String.format(Locale.getDefault(), "%.0fpx", currentTextSize));
        
        tvReaderBody.setTextSize(currentTextSize);
        tvContentChapterTitle.setTextSize(currentTextSize + 10);
    }

    private void applyTheme(String bgColor, String textColor) {
        int bgInt = Color.parseColor(bgColor);
        int textInt = Color.parseColor(textColor);
        
        scrollContent.setBackgroundColor(bgInt);
        findViewById(R.id.appbar).setBackgroundColor(bgInt);
        bottomContainer.setBackgroundColor(bgInt);
        
        tvToolbarTitle.setTextColor(textInt);
        tvToolbarChapter.setTextColor(textInt);
        tvContentChapterTitle.setTextColor(textInt);
        tvReaderBody.setTextColor(textInt);
        
        Toast.makeText(this, "Đã đổi chủ đề", Toast.LENGTH_SHORT).show();
    }

    private void toggleSettingsPanel() {
        if (settingsPanel.getVisibility() == View.VISIBLE) {
            settingsPanel.setVisibility(View.GONE);
        } else {
            settingsPanel.setVisibility(View.VISIBLE);
            Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
            settingsPanel.startAnimation(slideUp);
        }
    }

    private void setupSelectionPopup() {
        tvReaderBody.setTextIsSelectable(true);
        tvReaderBody.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                selectionPopup.setVisibility(View.VISIBLE);
                return true;
            }
            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }
            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }
            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                selectionPopup.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btn_copy).setOnClickListener(v -> {
            int start = tvReaderBody.getSelectionStart();
            int end = tvReaderBody.getSelectionEnd();
            if (start >= 0 && end > start) {
                String selectedText = tvReaderBody.getText().toString().substring(start, end);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ebook_text", selectedText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show();
            }
            selectionPopup.setVisibility(View.GONE);
        });
        
        View btnSaveNote = findViewById(R.id.btn_save_note);
        if (btnSaveNote != null) {
            btnSaveNote.setOnClickListener(v -> {
                int start = tvReaderBody.getSelectionStart();
                int end = tvReaderBody.getSelectionEnd();
                if (start >= 0 && end > start) {
                    String selectedText = tvReaderBody.getText().toString().substring(start, end);
                    addBookmarkWithNote(selectedText);
                }
                selectionPopup.setVisibility(View.GONE);
            });
        }

        findViewById(R.id.btn_dictionary).setOnClickListener(v -> {
            Toast.makeText(this, "Tra từ điển...", Toast.LENGTH_SHORT).show();
            selectionPopup.setVisibility(View.GONE);
        });
        
        findViewById(R.id.color_green).setOnClickListener(v -> highlightText("#A8E063"));
        findViewById(R.id.color_pink).setOnClickListener(v -> highlightText("#FF6B9D"));
        findViewById(R.id.color_blue).setOnClickListener(v -> highlightText("#2575FC"));
    }

    private void highlightText(String color) {
        Toast.makeText(this, "Đã highlight với màu " + color, Toast.LENGTH_SHORT).show();
        selectionPopup.setVisibility(View.GONE);
    }

    private void addBookmark() {
        addBookmarkWithNote("Đánh dấu trang");
    }

    private void addBookmarkWithNote(String noteText) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("page_number", 1);
            json.put("note", noteText);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(EbookReaderActivity.this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}