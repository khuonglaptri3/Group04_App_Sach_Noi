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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class EbookDetailActivity extends BaseBookDetailActivity {

    private LinearLayout llChaptersContainer;
    private View btnViewAllChapters;
    private List<String> chapterTitles = new ArrayList<>();

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_book_detail;
    }

    @Override
    protected void setupAdditionalUI() {
        // Inflate TOC for Ebook
        FrameLayout containerToc = findViewById(R.id.container_toc);
        if (containerToc != null) {
            View tocView = LayoutInflater.from(this).inflate(R.layout.layout_toc_ebook, containerToc, false);
            containerToc.addView(tocView);
            
            llChaptersContainer = tocView.findViewById(R.id.ll_chapters_container);
            btnViewAllChapters = tocView.findViewById(R.id.btn_view_all_chapters);
            
            btnViewAllChapters.setOnClickListener(v -> showAllChaptersBottomSheet());
        }
        
        // Hide Audio buttons for purely ebook
        View btnPreview = findViewById(R.id.btn_preview);
        if (btnPreview != null) {
            btnPreview.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onBookLoaded(Book book) {
        if (book.getEpubUrl() != null && !book.getEpubUrl().isEmpty()) {
            fetchEbookChapters();
        } else {
            runOnUiThread(() -> {
                chapterTitles.clear();
                renderChapters();
            });
        }
        
        // Update Read button to span full width if Audio is hidden
        View btnRead = findViewById(R.id.btn_read);
        if (btnRead != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnRead.getLayoutParams();
            params.setMarginStart(0);
            btnRead.setLayoutParams(params);
        }
    }
    
    private void fetchEbookChapters() {
        // We reuse book_chapters table for TOC if they exist
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
                        chapterTitles.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            chapterTitles.add(obj.optString("title", "Chương " + (i + 1)));
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
        
        if (chapterTitles.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Chưa có thông tin mục lục.");
            tvEmpty.setPadding(0, 16, 0, 16);
            llChaptersContainer.addView(tvEmpty);
            btnViewAllChapters.setVisibility(View.GONE);
            return;
        }
        
        ((View)llChaptersContainer.getParent()).setVisibility(View.VISIBLE);
        llChaptersContainer.removeAllViews();
        
        int displayCount = Math.min(3, chapterTitles.size());
        
        for (int i = 0; i < displayCount; i++) {
            String title = chapterTitles.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_toc_ebook, llChaptersContainer, false);
            
            TextView tvTitle = itemView.findViewById(R.id.tv_chapter_title);
            tvTitle.setText(title);
            final int index = i;
            itemView.setOnClickListener(v -> readChapter(index));
            
            llChaptersContainer.addView(itemView);
        }
        
        if (chapterTitles.size() > 3) {
            btnViewAllChapters.setVisibility(View.VISIBLE);
            if (btnViewAllChapters instanceof TextView) {
                ((TextView) btnViewAllChapters).setText("Xem tất cả " + chapterTitles.size() + " chương");
            }
        } else {
            btnViewAllChapters.setVisibility(View.GONE);
        }
    }
    
    private void showAllChaptersBottomSheet() {
        if (chapterTitles.isEmpty()) return;
        com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet bottomSheet = 
            new com.example.a23110035_23110060.view.bottomsheet.EbookChaptersBottomSheet(chapterTitles, index -> {
                readChapter(index);
            });
        bottomSheet.show(getSupportFragmentManager(), "TableOfContents");
    }
    
    private void readChapter(int index) {
        if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
            Intent intent = new Intent(this, PremiumActivity.class);
            startActivity(intent); // Note: Should probably use premiumLauncher but this is a simplified version
            return;
        }
        Intent intent = new Intent(this, EbookReaderActivity.class);
        intent.putExtra("bookId", bookId);
        // Could pass index to seek to specific chapter in EbookReaderActivity
        startActivity(intent);
    }
}
