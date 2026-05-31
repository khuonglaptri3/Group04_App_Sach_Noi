package com.example.a23110035_23110060;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.core.graphics.ColorUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private OkHttpClient client;

    private View selectionPopup, brightnessOverlay;
    private LinearLayout bottomContainer;
    private TextView tvFontSizeDisplay, tvToolbarTitle, tvToolbarChapter;
    private ViewPager2 vpEbook;
    private EbookPagerAdapter pagerAdapter;

    private float currentTextSize = 18f;
    private int currentTextColor = 0xFF1D1B20;
    private boolean isNightMode = false;

    private List<CharSequence> pages = new ArrayList<>();
    private List<Integer> chapterStartPages = new ArrayList<>();
    private List<String> chapterTitles = new ArrayList<>();
    private JSONArray cachedChapters = new JSONArray();
    private List<JSONObject> bookHighlights = new ArrayList<>();
    private java.util.Set<Integer> bookmarkedPages = new java.util.HashSet<>();
    private Thread paginationThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_reader);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appbar), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, insets.top, 0, 0);
            return windowInsets;
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_container), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        sessionManager = new SessionManager(this);
        client = NetworkClient.getClient(this);
        bookId = getIntent().getStringExtra("bookId");

        initViews();
        setupListeners();
        fetchBookContent();
    }

    private void initViews() {
        bottomContainer = findViewById(R.id.bottom_container);
        selectionPopup = findViewById(R.id.selection_popup);
        brightnessOverlay = findViewById(R.id.view_brightness_overlay);
        
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvToolbarChapter = findViewById(R.id.tv_toolbar_chapter);
        
        vpEbook = findViewById(R.id.vp_ebook);
        vpEbook.setOffscreenPageLimit(1);
        pagerAdapter = new EbookPagerAdapter(pages);
        pagerAdapter.setTextColor(currentTextColor);
        vpEbook.setAdapter(pagerAdapter);

        vpEbook.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateProgressUI(position);
            }
        });
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_bookmark).setOnClickListener(v -> saveBookmark());
        
        findViewById(R.id.btn_search).setOnClickListener(v -> showSearchDialog());
        findViewById(R.id.btn_contents).setOnClickListener(v -> showTableOfContents());
        
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

        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            EbookSettingsBottomSheet bottomSheet = new EbookSettingsBottomSheet(new EbookSettingsBottomSheet.SettingsListener() {
                @Override
                public void onBrightnessChanged(int progress) {
                    float alpha = (100 - progress) / 100f * 0.8f;
                    brightnessOverlay.setVisibility(alpha > 0 ? View.VISIBLE : View.GONE);
                    brightnessOverlay.setAlpha(alpha);
                }

                @Override
                public void onFontSizeAdjusted(int delta) {
                    adjustFontSize(delta);
                }

                @Override
                public void onThemeChanged(String bgColor, String textColor) {
                    applyTheme(bgColor, textColor);
                }
            }, currentTextSize);
            bottomSheet.show(getSupportFragmentManager(), "EbookSettings");
        });

        findViewById(R.id.btn_notes).setOnClickListener(v -> {
            BookNotesFragment fragment = BookNotesFragment.newInstance(bookId);
            fragment.show(getSupportFragmentManager(), "BookNotesFragment");
        });

        setupSelectionPopup();
    }

    private void fetchBookContent() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_files?book_id=eq." + bookId + "&file_type=eq.epub";
        String token = sessionManager.getAccessToken();
        String authHeader = (token != null) ? "Bearer " + token : "Bearer " + BuildConfig.SUPABASE_ANON_KEY;
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeader)
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
                            String fileUrl = array.getJSONObject(0).getString("file_url");
                            runOnUiThread(() -> Toast.makeText(EbookReaderActivity.this, "Đang tải sách...", Toast.LENGTH_SHORT).show());
                            
                            EpubExtractor.fetchAndParseEpub(fileUrl, getCacheDir(), new EpubExtractor.EpubCallback() {
                                @Override
                                public void onSuccess(List<Chapter> chapters) {
                                    try {
                                        JSONArray jsonChapters = new JSONArray();
                                        for (Chapter c : chapters) {
                                            JSONObject obj = new JSONObject();
                                            obj.put("title", c.getTitle());
                                            obj.put("text_content", c.getTextContent());
                                            jsonChapters.put(obj);
                                        }
                                        cachedChapters = jsonChapters;
                                        paginateText(jsonChapters);
                                        fetchBookmarks();
                                        fetchHighlights();
                                        fetchLastProgress();
                                    } catch (Exception ex) {
                                        fetchLegacyDescription();
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> Toast.makeText(EbookReaderActivity.this, "Lỗi giải nén EPUB", Toast.LENGTH_SHORT).show());
                                    fetchLegacyDescription();
                                }
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            JSONObject obj = array.getJSONObject(0);
                            String title = obj.getString("title");
                            String content = obj.optString("description", "Không có nội dung.");

                            runOnUiThread(() -> tvToolbarTitle.setText(title));

                            JSONArray mockChapters = new JSONArray();
                            JSONObject mockChapter = new JSONObject();
                            mockChapter.put("title", "Giới thiệu");
                            mockChapter.put("text_content", content);
                            mockChapters.put(mockChapter);
                            paginateText(mockChapters);
                        }
                    } catch (Exception e) {
                        Log.e("EbookReader", "Parse content error", e);
                    }
                }
            }
        });
    }

    private void paginateText(JSONArray chaptersArray) {
        if (paginationThread != null && paginationThread.isAlive()) {
            paginationThread.interrupt();
        }

        vpEbook.post(() -> {
            paginationThread = new Thread(() -> {
                try {
                    TextPaint paint = new TextPaint();
                    paint.setTextSize(currentTextSize * getResources().getDisplayMetrics().scaledDensity);
                    paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
                    
                    int width = vpEbook.getWidth() - dpToPx(48); // 24dp horizontal padding * 2
                    int height = vpEbook.getHeight() - dpToPx(48); // 24dp vertical padding * 2
                
                    if (width <= 0 || height <= 0) {
                        width = getResources().getDisplayMetrics().widthPixels - dpToPx(48);
                        height = getResources().getDisplayMetrics().heightPixels - dpToPx(200);
                    }
                    
                    List<CharSequence> newPages = new ArrayList<>();
                    List<Integer> newChapterStarts = new ArrayList<>();
                    List<String> newChapterTitles = new ArrayList<>();
                    
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        if (Thread.interrupted()) return;
                        JSONObject obj = chaptersArray.getJSONObject(i);
                        String title = obj.optString("title", "Chương " + (i + 1));
                        String text = obj.optString("text_content", "");
                        String content = title.toUpperCase() + "\n\n" + text;
                        
                        newChapterStarts.add(newPages.size());
                        newChapterTitles.add(title);
                        
                        StaticLayout layout = StaticLayout.Builder.obtain(content, 0, content.length(), paint, width)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(dpToPx(10), 1.0f)
                                .setIncludePad(false)
                                .build();
                        
                        int startOffset = 0;
                        int lineCount = layout.getLineCount();
                        int pageStartLine = 0;
                        
                        for (int line = 0; line < lineCount; line++) {
                            int bottom = layout.getLineBottom(line);
                            int top = layout.getLineTop(pageStartLine);
                            if (bottom - top > height) {
                                int safeLineIndex = Math.max(0, line - 1);
                                if (line == pageStartLine) {
                                    // Current line itself is taller than height, must include it to avoid infinite loop
                                    safeLineIndex = line;
                                }
                                int endOffset = layout.getLineEnd(safeLineIndex);
                                if (endOffset > startOffset) {
                                    newPages.add(content.substring(startOffset, endOffset));
                                    startOffset = endOffset;
                                    pageStartLine = (line == pageStartLine) ? line + 1 : line;
                                }
                            }
                        }
                        if (startOffset < content.length()) {
                            newPages.add(content.substring(startOffset));
                        }
                    }
                    
                    runOnUiThread(() -> {
                        pages.clear();
                        pages.addAll(newPages);
                        chapterStartPages.clear();
                        chapterStartPages.addAll(newChapterStarts);
                        chapterTitles.clear();
                        chapterTitles.addAll(newChapterTitles);
                        
                        pagerAdapter.updatePages(pages);
                        applyHighlightsToPages();
                        updateProgressUI(vpEbook.getCurrentItem());
                    });
                } catch (Exception e) {
                    Log.e("Pagination", "Error", e);
                }
            });
            paginationThread.start();
        });
    }

    private void updateProgressUI(int position) {
        if (pages.isEmpty()) return;
        
        int percentage = (int) (((float) (position + 1) / pages.size()) * 100);
        TextView tvPercent = findViewById(R.id.tv_percent_read);
        com.google.android.material.progressindicator.LinearProgressIndicator pbReading = findViewById(R.id.pb_reading);
        
        tvPercent.setText(percentage + "% đã đọc");
        pbReading.setProgress(percentage);
        
        TextView tvPage = findViewById(R.id.tv_page_info);
        tvPage.setText("Trang " + (position + 1) + " / " + pages.size());
        
        int currentChapterIndex = 0;
        for (int i = 0; i < chapterStartPages.size(); i++) {
            if (position >= chapterStartPages.get(i)) {
                currentChapterIndex = i;
            } else {
                break;
            }
        }
        if (currentChapterIndex < chapterTitles.size()) {
            tvToolbarChapter.setText(chapterTitles.get(currentChapterIndex));
        }
        
        android.widget.ImageButton btnBookmark = findViewById(R.id.btn_bookmark);
        if (bookmarkedPages.contains(position + 1)) {
            btnBookmark.setColorFilter(Color.parseColor("#FFC107"));
        } else {
            btnBookmark.clearColorFilter();
        }
    }

    private void fetchHighlights() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_highlights?user_id=eq." + userId + "&book_id=eq." + bookId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("EbookReader", "Fetch highlights failed", e);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray arr = new JSONArray(responseData);
                        bookHighlights.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            bookHighlights.add(arr.getJSONObject(i));
                        }
                        runOnUiThread(() -> applyHighlightsToPages());
                    } catch (Exception e) {
                        Log.e("EbookReader", "Parse highlights failed", e);
                    }
                }
            }
        });
    }

    private void applyHighlightsToPages() {
        if (pages == null || pages.isEmpty()) return;
        
        for (JSONObject highlight : bookHighlights) {
            try {
                int pageNum = highlight.getInt("page_number") - 1;
                if (pageNum >= 0 && pageNum < pages.size()) {
                    int start = highlight.getInt("start_offset");
                    int end = highlight.getInt("end_offset");
                    String color = highlight.getString("color");
                    
                    CharSequence pageText = pages.get(pageNum);
                    android.text.SpannableString spannable;
                    if (pageText instanceof android.text.SpannableString) {
                        spannable = (android.text.SpannableString) pageText;
                    } else {
                        spannable = new android.text.SpannableString(pageText);
                        pages.set(pageNum, spannable);
                    }
                    
                    if (start >= 0 && end <= spannable.length() && start < end) {
                        spannable.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor(color)), 
                                        start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } catch (Exception e) {
                Log.e("EbookReader", "Error applying highlight", e);
            }
        }
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
    }

    private void fetchBookmarks() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&book_id=eq." + bookId + "&select=page_number";
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
                        JSONArray arr = new JSONArray(response.body().string());
                        bookmarkedPages.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.has("page_number") && !obj.isNull("page_number")) {
                                bookmarkedPages.add(obj.getInt("page_number"));
                            }
                        }
                        runOnUiThread(() -> updateProgressUI(vpEbook.getCurrentItem()));
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void saveBookmark() {
        if (pages.isEmpty()) return;
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentPage = vpEbook.getCurrentItem() + 1;
        
        if (bookmarkedPages.contains(currentPage)) {
            // Remove bookmark
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&book_id=eq." + bookId + "&page_number=eq." + currentPage;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .delete()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        bookmarkedPages.remove(currentPage);
                        runOnUiThread(() -> updateProgressUI(vpEbook.getCurrentItem()));
                    }
                }
            });
        } else {
            // Add bookmark
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks";
            JSONObject json = new JSONObject();
            try {
                json.put("user_id", userId);
                json.put("book_id", bookId);
                json.put("page_number", currentPage);
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
                        bookmarkedPages.add(currentPage);
                        runOnUiThread(() -> updateProgressUI(vpEbook.getCurrentItem()));
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoSaveProgress();
    }

    private void autoSaveProgress() {
        if (pages.isEmpty()) return;
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        int percent = (int) (((float) (vpEbook.getCurrentItem() + 1) / pages.size()) * 100);

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/progress?user_id=eq." + userId + "&book_id=eq." + bookId;
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("last_page_number", vpEbook.getCurrentItem() + 1);
            json.put("percent_complete", percent);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }

    private void fetchLastProgress() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/progress?user_id=eq." + userId + "&book_id=eq." + bookId + "&select=last_page_number";
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
                        JSONArray arr = new JSONArray(response.body().string());
                        if (arr.length() > 0) {
                            int lastPage = arr.getJSONObject(0).optInt("last_page_number", 1) - 1;
                            if (lastPage > 0 && lastPage < pages.size()) {
                                runOnUiThread(() -> vpEbook.setCurrentItem(lastPage, false));
                            }
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void showTableOfContents() {
        if (chapterTitles.isEmpty()) {
            Toast.makeText(this, "Không có mục lục", Toast.LENGTH_SHORT).show();
            return;
        }
        EbookChaptersBottomSheet bottomSheet = new EbookChaptersBottomSheet(chapterTitles, index -> {
            if (index < chapterStartPages.size()) {
                vpEbook.setCurrentItem(chapterStartPages.get(index), false);
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "EbookChapters");
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập từ khóa");
        input.setPadding(48, 32, 48, 32);
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tìm kiếm")
            .setView(input)
            .setPositiveButton("Tìm", (dialog, which) -> {
                String query = input.getText().toString().toLowerCase();
                if (query.isEmpty()) return;
                
                for (int i = vpEbook.getCurrentItem(); i < pages.size(); i++) {
                    if (pages.get(i).toString().toLowerCase().contains(query)) {
                        vpEbook.setCurrentItem(i, false);
                        Toast.makeText(this, "Đã tìm thấy ở trang " + (i + 1), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                for (int i = 0; i < vpEbook.getCurrentItem(); i++) {
                    if (pages.get(i).toString().toLowerCase().contains(query)) {
                        vpEbook.setCurrentItem(i, false);
                        Toast.makeText(this, "Đã tìm thấy ở trang " + (i + 1), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(this, "Không tìm thấy", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void adjustFontSize(int delta) {
        currentTextSize = Math.max(14, Math.min(28, currentTextSize + delta));
        pagerAdapter.setTextSize(currentTextSize);
        if (cachedChapters.length() > 0) {
            paginateText(cachedChapters);
        }
    }

    private void applyTheme(String bgColor, String textColor) {
        int bgInt = Color.parseColor(bgColor);
        int textInt = Color.parseColor(textColor);
        currentTextColor = textInt;

        findViewById(R.id.main_layout).setBackgroundColor(bgInt);
        vpEbook.setBackgroundColor(bgInt);
        findViewById(R.id.appbar).setBackgroundColor(bgInt);
        bottomContainer.setBackgroundColor(bgInt);
        
        tvToolbarTitle.setTextColor(textInt);
        
        // Muted text color for secondary info
        int mutedTextInt = isDark(textInt) ? 0xFF49454F : 0xFFB0B0B0;
        tvToolbarChapter.setTextColor(mutedTextInt);
        
        TextView tvPageInfo = findViewById(R.id.tv_page_info);
        TextView tvPercentRead = findViewById(R.id.tv_percent_read);
        if (tvPageInfo != null) tvPageInfo.setTextColor(mutedTextInt);
        if (tvPercentRead != null) tvPercentRead.setTextColor(mutedTextInt);
        
        if (pagerAdapter != null) {
            pagerAdapter.setTextColor(textInt);
        }
        
        // Update icons based on brightness of textColor
        int iconColor = isDark(textInt) ? 0xFF49454F : 0xFFE0E0E0;
        ((android.widget.ImageButton)findViewById(R.id.btn_back)).setColorFilter(iconColor);
        ((android.widget.ImageButton)findViewById(R.id.btn_search)).setColorFilter(iconColor);
        ((android.widget.ImageButton)findViewById(R.id.btn_bookmark)).setColorFilter(iconColor);
        ((android.widget.ImageButton)findViewById(R.id.btn_more)).setColorFilter(iconColor);

        // Update bottom bar icons and text
        int[] bottomButtons = {R.id.btn_contents, R.id.btn_night_mode, R.id.btn_settings, R.id.btn_notes};
        for (int id : bottomButtons) {
            LinearLayout btn = findViewById(id);
            if (btn != null) {
                for (int i = 0; i < btn.getChildCount(); i++) {
                    View child = btn.getChildAt(i);
                    if (child instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) child).setColorFilter(iconColor);
                    } else if (child instanceof TextView) {
                        ((TextView) child).setTextColor(iconColor);
                    }
                }
            }
        }

        // Update Status Bar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgInt);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                if (isDark(textInt)) { // Light theme
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else { // Dark theme
                    decor.setSystemUiVisibility(0);
                }
            }
        }

        Toast.makeText(this, "Đã đổi chủ đề", Toast.LENGTH_SHORT).show();
    }

    private boolean isDark(int color) {
        return ColorUtils.calculateLuminance(color) < 0.5;
    }


    private TextView getCurrentTextView() {
        View pageView = vpEbook.getChildAt(0);
        if (pageView instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) pageView;
            View currentView = rv.getLayoutManager().findViewByPosition(vpEbook.getCurrentItem());
            if (currentView != null) {
                return currentView.findViewById(R.id.tv_page_content);
            }
        }
        return null;
    }

    private void setupSelectionPopup() {
        android.view.ActionMode.Callback customCallback = new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.clear(); // Hide default menu
                return true; 
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.clear();
                selectionPopup.setVisibility(View.VISIBLE);
                
                // Try to position popup above text
                TextView tv = getCurrentTextView();
                if (tv != null) {
                    int start = tv.getSelectionStart();
                    if (start >= 0) {
                        Layout layout = tv.getLayout();
                        if (layout != null) {
                            int line = layout.getLineForOffset(start);
                            int y = layout.getLineTop(line);
                            float adjustedY = y + vpEbook.getY() - dpToPx(55);
                            selectionPopup.setY(Math.max(dpToPx(55), adjustedY)); // keep within bounds
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                selectionPopup.setVisibility(View.GONE);
            }
        };
        pagerAdapter.setCustomSelectionCallback(customCallback);

        findViewById(R.id.btn_copy).setOnClickListener(v -> {
            TextView tv = getCurrentTextView();
            if (tv != null) {
                int start = tv.getSelectionStart();
                int end = tv.getSelectionEnd();
                if (start >= 0 && end > start) {
                    String selectedText = tv.getText().toString().substring(start, end);
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("ebook_text", selectedText);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show();
                }
            }
            selectionPopup.setVisibility(View.GONE);
        });
        
        View btnSaveNote = findViewById(R.id.btn_save_note);
        if (btnSaveNote != null) {
            btnSaveNote.setOnClickListener(v -> {
                TextView tv = getCurrentTextView();
                if (tv != null) {
                    int start = tv.getSelectionStart();
                    int end = tv.getSelectionEnd();
                    if (start >= 0 && end > start) {
                        String selectedText = tv.getText().toString().substring(start, end);
                        showNoteInputDialog(selectedText, start, end);
                    }
                }
                selectionPopup.setVisibility(View.GONE);
            });
        }

        findViewById(R.id.color_green).setOnClickListener(v -> highlightText("#A8E063"));
        findViewById(R.id.color_pink).setOnClickListener(v -> highlightText("#FF6B9D"));
        findViewById(R.id.color_blue).setOnClickListener(v -> highlightText("#2575FC"));
    }

    private void highlightText(String color) {
        TextView tv = getCurrentTextView();
        if (tv != null) {
            int start = tv.getSelectionStart();
            int end = tv.getSelectionEnd();
            if (start >= 0 && end > start) {
                String selectedText = tv.getText().toString().substring(start, end);
                android.text.SpannableString spannable = new android.text.SpannableString(tv.getText());
                spannable.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor(color)), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(spannable);
                pages.set(vpEbook.getCurrentItem(), spannable); // Save the span in memory
                
                saveHighlightToDb(color, selectedText, start, end);
                
                try {
                    JSONObject h = new JSONObject();
                    h.put("page_number", vpEbook.getCurrentItem() + 1);
                    h.put("color", color);
                    h.put("start_offset", start);
                    h.put("end_offset", end);
                    h.put("highlighted_text", selectedText);
                    bookHighlights.add(h);
                } catch (Exception e) {}

                Toast.makeText(this, "Đã đánh dấu màu", Toast.LENGTH_SHORT).show();
            }
        }
        selectionPopup.setVisibility(View.GONE);
    }

    private void saveHighlightToDb(String color, String highlightedText, int startOffset, int endOffset) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_highlights";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("page_number", vpEbook.getCurrentItem() + 1);
            json.put("color", color);
            json.put("highlighted_text", highlightedText);
            json.put("start_offset", startOffset);
            json.put("end_offset", endOffset);
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
            public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }

    private void showNoteInputDialog(String selectedText, int startOffset, int endOffset) {
        EditText input = new EditText(this);
        input.setHint("Nhập nội dung ghi chú");
        input.setPadding(48, 32, 48, 32);
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Thêm Ghi chú")
            .setView(input)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String note = input.getText().toString().trim();
                if (!note.isEmpty()) {
                    saveHighlightAndNote(note, selectedText, startOffset, endOffset);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void saveHighlightAndNote(String noteText, String highlightedText, int startOffset, int endOffset) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        // Apply highlight locally immediately
        runOnUiThread(() -> {
            TextView tv = getCurrentTextView();
            if (tv != null) {
                android.text.SpannableString spannable = new android.text.SpannableString(tv.getText());
                spannable.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor("#FFC107")), 
                                 startOffset, endOffset, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(spannable);
                pages.set(vpEbook.getCurrentItem(), spannable);
            }
            
            try {
                JSONObject h = new JSONObject();
                h.put("page_number", vpEbook.getCurrentItem() + 1);
                h.put("color", "#FFC107");
                h.put("start_offset", startOffset);
                h.put("end_offset", endOffset);
                h.put("highlighted_text", highlightedText);
                bookHighlights.add(h);
            } catch (Exception e) {}
        });

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_highlights";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("page_number", vpEbook.getCurrentItem() + 1);
            json.put("color", "#FFC107"); // Default color for notes
            json.put("highlighted_text", highlightedText);
            json.put("start_offset", startOffset);
            json.put("end_offset", endOffset);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        if (arr.length() > 0) {
                            String highlightId = arr.getJSONObject(0).getString("id");
                            saveNoteToDb(highlightId, noteText);
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void saveNoteToDb(String highlightId, String noteText) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_notes";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("highlight_id", highlightId);
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public void jumpToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            vpEbook.setCurrentItem(pageIndex, false);
        }
    }
}