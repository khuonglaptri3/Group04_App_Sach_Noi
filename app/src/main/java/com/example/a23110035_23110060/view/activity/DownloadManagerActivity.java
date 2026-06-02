package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.adapter.DownloadAdapter;

import com.example.a23110035_23110060.controller.SessionManager;
import com.example.a23110035_23110060.helper.DownloadHelper;
import com.example.a23110035_23110060.model.DownloadItem;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManagerActivity extends AppCompatActivity {

    private RecyclerView rvDownloads;
    private LinearLayout layoutEmptyDownloads;
    private ProgressBar progressBar;
    private DownloadAdapter adapter;
    private List<DownloadItem> downloadList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvDownloads = findViewById(R.id.rvDownloads);
        layoutEmptyDownloads = findViewById(R.id.layoutEmptyDownloads);
        progressBar = findViewById(R.id.progressBar);

        rvDownloads.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadAdapter(downloadList, (item, position) -> {
            boolean deleted = DownloadHelper.deleteDownloadedFile(this, item.getBookId(), item.getFileType());
            if (deleted) {
                downloadList.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(this, "Đã xóa file", Toast.LENGTH_SHORT).show();
                
                // Update Supabase
                updateDownloadStatus(item.getBookId(), false);

                if (downloadList.isEmpty()) {
                    rvDownloads.setVisibility(View.GONE);
                    layoutEmptyDownloads.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(this, "Không thể xóa file", Toast.LENGTH_SHORT).show();
            }
        });
        rvDownloads.setAdapter(adapter);

        loadDownloadedBooks();
    }

    private void loadDownloadedBooks() {
        progressBar.setVisibility(View.VISIBLE);
        rvDownloads.setVisibility(View.GONE);
        layoutEmptyDownloads.setVisibility(View.GONE);

        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            progressBar.setVisibility(View.GONE);
            layoutEmptyDownloads.setVisibility(View.VISIBLE);
            return;
        }

        // Fetch user library where is_downloaded is true
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + "&is_downloaded=eq.true&select=book_id,books(title,cover_url,is_audiobook,is_ebook)";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    layoutEmptyDownloads.setVisibility(View.VISIBLE);
                    Toast.makeText(DownloadManagerActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        downloadList.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            String bookId = item.getString("book_id");
                            JSONObject book = item.getJSONObject("books");
                            String title = book.getString("title");
                            String coverUrl = book.optString("cover_url", "");
                            boolean isAudio = book.optBoolean("is_audiobook", false);
                            boolean isEbook = book.optBoolean("is_ebook", false);

                            // Check local files
                            if (isAudio) {
                                File audioFile = DownloadHelper.getDownloadedFile(DownloadManagerActivity.this, bookId, "audio");
                                if (audioFile != null) {
                                    downloadList.add(new DownloadItem(bookId, title, "audio", coverUrl, audioFile.length()));
                                }
                            }
                            if (isEbook) {
                                File ebookFile = DownloadHelper.getDownloadedFile(DownloadManagerActivity.this, bookId, "ebook");
                                if (ebookFile != null) {
                                    downloadList.add(new DownloadItem(bookId, title, "ebook", coverUrl, ebookFile.length()));
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (downloadList.isEmpty()) {
                                layoutEmptyDownloads.setVisibility(View.VISIBLE);
                            } else {
                                rvDownloads.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            layoutEmptyDownloads.setVisibility(View.VISIBLE);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        layoutEmptyDownloads.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void updateDownloadStatus(String bookId, boolean isDownloaded) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        // Check if there are still other files downloaded for this book (e.g. deleted audio but kept ebook)
        boolean hasOtherFile = false;
        for (DownloadItem item : downloadList) {
            if (item.getBookId().equals(bookId)) {
                hasOtherFile = true;
                break;
            }
        }
        
        if (hasOtherFile && !isDownloaded) {
            // Do not update is_downloaded=false if another file type is still downloaded
            return;
        }

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + "&book_id=eq." + bookId;
        
        JSONObject body = new JSONObject();
        try {
            body.put("is_downloaded", isDownloaded);
        } catch (Exception e) {}

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                body.toString(),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .patch(requestBody) // Use PATCH to update existing record
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
        });
    }
}
