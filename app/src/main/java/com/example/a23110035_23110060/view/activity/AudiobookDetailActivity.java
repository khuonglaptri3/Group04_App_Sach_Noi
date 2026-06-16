package com.example.a23110035_23110060.view.activity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;
import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.model.Chapter;
import com.example.a23110035_23110060.controller.PlayerManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class AudiobookDetailActivity extends BaseBookDetailActivity {

    private LinearLayout llChaptersContainer;
    private View btnViewAllChapters;
    private List<Chapter> chapterList = new ArrayList<>();

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_book_detail;
    }

    @Override
    protected void setupAdditionalUI() {
        // TOC removed from Book Detail
        
        // Hide Ebook button for purely audiobook
        View btnRead = findViewById(R.id.btn_read);
        if (btnRead != null) {
            btnRead.setVisibility(View.GONE);
        }
        
        // Update Preview (Audio) button to span full width
        View btnPreview = findViewById(R.id.btn_preview);
        if (btnPreview != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnPreview.getLayoutParams();
            params.setMarginEnd(0);
            btnPreview.setLayoutParams(params);
        }
    }

    @Override
    protected void onBookLoaded(Book book) {
        if (book.getAudioUrl() != null && !book.getAudioUrl().isEmpty()) {
            fetchAudiobookChapters();
        } else {
            runOnUiThread(() -> {
                chapterList.clear();
                renderChapters();
            });
        }
    }
    
    private void fetchAudiobookChapters() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_chapters?book_id=eq." + bookId + "&order=chapter_index.asc";
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
                        chapterList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Chapter chapter = new Chapter();
                            chapter.setId(obj.getString("id"));
                            chapter.setBookId(obj.getString("book_id"));
                            chapter.setChapterNumber(obj.getInt("chapter_index"));
                            chapter.setTitle(obj.optString("title"));
                            chapter.setStartTime(obj.optInt("start_time_seconds", 0) * 1000);
                            chapter.setEndTime(obj.optInt("end_time_seconds", 0) * 1000);
                            chapter.setAudioUrl(currentBook.getAudioUrl());
                            chapterList.add(chapter);
                        }
                        
                        runOnUiThread(() -> renderChapters());
                    } catch (Exception e) {}
                }
            }
        });
    }
    
    private void renderChapters() {
        if (llChaptersContainer == null) return;
        
        ((View)llChaptersContainer.getParent()).setVisibility(View.VISIBLE);
        llChaptersContainer.removeAllViews();
        
        if (chapterList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Chưa có thông tin mục lục.");
            tvEmpty.setPadding(0, 16, 0, 16);
            llChaptersContainer.addView(tvEmpty);
            btnViewAllChapters.setVisibility(View.GONE);
            return;
        }
        
        ((View)llChaptersContainer.getParent()).setVisibility(View.VISIBLE);
        llChaptersContainer.removeAllViews();
        
        int displayCount = Math.min(3, chapterList.size());
        
        for (int i = 0; i < displayCount; i++) {
            Chapter chapter = chapterList.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_toc_audiobook, llChaptersContainer, false);
            
            TextView tvTitle = itemView.findViewById(R.id.tv_chapter_title);
            TextView tvDuration = itemView.findViewById(R.id.tv_chapter_duration);
            
            tvTitle.setText(chapter.getTitle());
            
            long durationMs = chapter.getEndTime() - chapter.getStartTime();
            if (durationMs > 0) {
                long durationMins = durationMs / 60000;
                tvDuration.setText(durationMins + " phút");
            } else {
                tvDuration.setVisibility(View.GONE);
            }
            
            itemView.setOnClickListener(v -> playChapter(chapterList, chapter));
            
            llChaptersContainer.addView(itemView);
        }
        
        if (chapterList.size() > 3) {
            btnViewAllChapters.setVisibility(View.VISIBLE);
            if (btnViewAllChapters instanceof TextView) {
                ((TextView) btnViewAllChapters).setText("Xem tất cả " + chapterList.size() + " chương");
            }
        } else {
            btnViewAllChapters.setVisibility(View.GONE);
        }
    }
    
    private void showAllChaptersBottomSheet() {
        if (chapterList.isEmpty()) return;
        List<String> titles = new ArrayList<>();
        for (Chapter c : chapterList) {
            titles.add(c.getTitle());
        }
        com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet bottomSheet = 
            new com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet(titles, new com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet.ChapterListener() {
                @Override
                public void onChapterSelected(int index) {
                    playChapter(chapterList, chapterList.get(index));
                }
            });
        bottomSheet.show(getSupportFragmentManager(), "TableOfContents");
    }
    
    private void playChapter(List<Chapter> chapters, Chapter targetChapter) {
        if (currentBook == null) return;
        PlayerManager.getInstance().playBook(currentBook);
        PlayerManager.getInstance().setChapters(chapters);
        // We could seek to the specific chapter, but for now just start AudioPlayerActivity
        startActivity(new Intent(this, AudioPlayerActivity.class));
    }
}
