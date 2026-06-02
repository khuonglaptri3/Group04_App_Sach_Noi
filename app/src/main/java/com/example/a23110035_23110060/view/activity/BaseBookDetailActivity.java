package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.bottomsheet.AuthorDetailBottomSheet;
import com.example.a23110035_23110060.view.adapter.ReviewAdapter;
import com.example.a23110035_23110060.view.adapter.BookAdapter;

import com.example.a23110035_23110060.controller.SessionManager;
import com.example.a23110035_23110060.controller.NetworkClient;
import com.example.a23110035_23110060.model.Chapter;
import com.example.a23110035_23110060.controller.PlayerManager;
import com.example.a23110035_23110060.helper.DownloadHelper;
import com.example.a23110035_23110060.model.Book;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class BaseBookDetailActivity extends AppCompatActivity {

    protected String bookId;
    protected Book currentBook;
    protected OkHttpClient client;
    protected SessionManager sessionManager;

    protected ImageView ivCover;
    protected TextView tvTitle, tvAuthor, tvRatingCount, tvDescription, btnReadMore;
    protected RatingBar rbBook;
    protected MaterialButton btnBuy, btnPreview, btnRead, btnFavorite;
    protected RecyclerView rvSimilarBooks;
    
    protected boolean isDescriptionExpanded = false;
    protected boolean isFavorite = false;
    protected boolean isBookInLibrary = false;
    protected boolean isUserPremium = false;
    protected View layoutWriteReview;
    protected RecyclerView rvReviews;
    protected TextView tvOverallRating, tvOverallRatingCount;
    protected ImageView ivRatingBookCover, ivUserAvatar;
    protected MaterialButton btnViewAllReviews;
    protected int currentReviewCount = 0;

    protected String authorBio;
    protected String authorAvatar;

    protected abstract int getLayoutResource();
    protected abstract void setupAdditionalUI();
    protected abstract void onBookLoaded(Book book);

    private final ActivityResultLauncher<Intent> writeReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchReviews();
                }
            }
    );

    private final ActivityResultLauncher<Intent> premiumLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchBookDetails(); // Refresh to get new premium status
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        sessionManager = new SessionManager(this);
        client = NetworkClient.getClient(this);
        bookId = getIntent().getStringExtra("bookId");

        initViews();
        setupListeners();
        setupAdditionalUI();
        fetchBookDetails();
    }

    protected void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivCover = findViewById(R.id.iv_book_cover);
        tvTitle = findViewById(R.id.tv_book_title);
        tvAuthor = findViewById(R.id.tv_author_name);
        tvRatingCount = findViewById(R.id.tv_rating_count);
        tvDescription = findViewById(R.id.tv_description);
        btnReadMore = findViewById(R.id.btn_read_more);
        rbBook = findViewById(R.id.rb_book);
        btnBuy = findViewById(R.id.btn_buy);
        btnPreview = findViewById(R.id.btn_preview);
        btnRead = findViewById(R.id.btn_read);
        btnFavorite = findViewById(R.id.btn_favorite);
        rvSimilarBooks = findViewById(R.id.rv_similar_books);
        rvReviews = findViewById(R.id.rv_reviews);
        layoutWriteReview = findViewById(R.id.layout_write_review);
        tvOverallRating = findViewById(R.id.tv_overall_rating);
        tvOverallRatingCount = findViewById(R.id.tv_overall_rating_count);
        ivRatingBookCover = findViewById(R.id.iv_rating_book_cover);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        btnViewAllReviews = findViewById(R.id.btn_view_all_reviews);
        View btnWriteReviewReal = findViewById(R.id.btn_write_review);

        rvSimilarBooks.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    protected void setupListeners() {
        findViewById(R.id.btn_share).setOnClickListener(v -> shareBook());

        tvAuthor.setOnClickListener(v -> {
            if (currentBook != null && currentBook.getAuthorId() != null) {
                AuthorDetailBottomSheet bottomSheet = AuthorDetailBottomSheet.newInstance(
                        currentBook.getAuthorId(),
                        currentBook.getAuthorName(),
                        authorBio,
                        authorAvatar
                );
                bottomSheet.show(getSupportFragmentManager(), "AuthorDetailBottomSheet");
            }
        });
        
        btnReadMore.setOnClickListener(v -> {
            isDescriptionExpanded = !isDescriptionExpanded;
            tvDescription.setMaxLines(isDescriptionExpanded ? Integer.MAX_VALUE : 3);
            btnReadMore.setText(isDescriptionExpanded ? "Thu gọn" : "Xem thêm");
        });

        btnPreview.setOnClickListener(v -> {
            if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
                Intent intent = new Intent(this, PremiumActivity.class);
                premiumLauncher.launch(intent);
                return;
            }
            if (currentBook != null && currentBook.getAudioUrl() != null) {
                fetchChaptersAndPlay();
            } else {
                Toast.makeText(this, "Sách này chưa có file âm thanh trong database", Toast.LENGTH_SHORT).show();
            }
        });

        btnBuy.setOnClickListener(v -> performPurchase());

        btnRead.setOnClickListener(v -> {
            if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
                Intent intent = new Intent(this, PremiumActivity.class);
                premiumLauncher.launch(intent);
                return;
            }
            Intent intent = new Intent(this, EbookReaderActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        MaterialButton btnDownload = findViewById(R.id.btn_download);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> handleDownloadClick());
        }

        findViewById(R.id.btn_write_review).setOnClickListener(v -> writeReview());
        
        btnViewAllReviews.setOnClickListener(v -> {
            if (currentBook != null) {
                Intent intent = new Intent(this, AllReviewsActivity.class);
                intent.putExtra("bookId", bookId);
                intent.putExtra("bookTitle", currentBook.getTitle());
                startActivity(intent);
            }
        });
    }

    protected void writeReview() {
        if (currentBook == null) return;
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("bookId", bookId);
        intent.putExtra("bookCoverUrl", currentBook.getCoverUrl());
        intent.putExtra("bookType", currentBook.isAudiobook() ? "audio" : "ebook");
        writeReviewLauncher.launch(intent);
    }
    
    protected void handleReadAction() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
        if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
            Intent intent = new Intent(this, PremiumActivity.class);
            premiumLauncher.launch(intent);
            return;
        }
        Intent intent = new Intent(this, EbookReaderActivity.class);
        intent.putExtra("bookId", bookId);
        startActivity(intent);
    }
    
    protected void checkDownloadStatus(String bookId, MaterialButton btnDownload) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }

        if (currentBook == null) return;
        if (currentBook.isPremiumOnly() && !isUserPremium) {
            Intent intent = new Intent(this, PremiumActivity.class);
            premiumLauncher.launch(intent);
            return;
        }

        boolean hasAudio = currentBook.getAudioUrl() != null && !currentBook.getAudioUrl().isEmpty();
        boolean hasEbook = currentBook.getEpubUrl() != null && !currentBook.getEpubUrl().isEmpty();

        if (hasAudio && hasEbook) {
            String[] options = {"Tải Sách nói (Audio)", "Tải Sách điện tử (Ebook)"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn định dạng tải xuống")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startDownload("audio", currentBook.getAudioUrl());
                    else startDownload("ebook", currentBook.getEpubUrl());
                })
                .show();
        } else if (hasAudio) {
            startDownload("audio", currentBook.getAudioUrl());
        } else if (hasEbook) {
            startDownload("ebook", currentBook.getEpubUrl());
        } else {
            Toast.makeText(this, "Sách này không có file để tải", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startDownload(String type, String url) {
        DownloadHelper.downloadFile(this, url, bookId, currentBook.getTitle(), type);
        
        // Update is_downloaded = true in Supabase
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String dbUrl = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + "&book_id=eq." + bookId;
        JSONObject body = new JSONObject();
        try {
            body.put("is_downloaded", true);
            body.put("user_id", userId);
            body.put("book_id", bookId);
        } catch (Exception e) {}

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                body.toString(),
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        // Upsert if not exists
        Request request = new Request.Builder()
                .url(dbUrl)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .patch(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response r) throws IOException {
                if (!r.isSuccessful() && r.code() == 404) {
                    // Row might not exist, need to insert instead
                    String insertUrl = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?on_conflict=user_id,book_id";
                    Request insertReq = new Request.Builder()
                        .url(insertUrl)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .post(requestBody)
                        .build();
                    client.newCall(insertReq).enqueue(new Callback() {
                        @Override public void onFailure(@NonNull Call c, @NonNull IOException ex) {}
                        @Override public void onResponse(@NonNull Call c, @NonNull Response res) throws IOException {}
                    });
                }
            }
        });
    }

    // submitReview removed as it is now handled by WriteReviewActivity

    protected void checkLibraryStatus() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=is_premium";
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
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            isUserPremium = array.getJSONObject(0).optBoolean("is_premium", false);
                            runOnUiThread(() -> updatePurchaseButtonUI());
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    protected void fetchBookDetails() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        // CẬP NHẬT TRUY VẤN: Lấy luôn cả book_files trong 1 lần gọi
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?id=eq." + bookId + 
                     "&select=*,authors(*),categories(name_vi),user_library(is_favorite,is_purchased),book_files(file_url,file_type)";
        
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
        
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        } else {
            builder.addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("BookDetail", "Fetch failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                             JSONObject obj = array.getJSONObject(0);
                             JSONObject authorObj = obj.optJSONObject("authors");
                             String authorName = "Unknown";
                             authorBio = null;
                             authorAvatar = null;
                             if (authorObj != null) {
                                 authorName = authorObj.optString("name", "Unknown");
                                 authorBio = authorObj.optString("biography", null);
                                 authorAvatar = authorObj.optString("avatar_url", null);
                             }
                             
                             currentBook = new Book();
                             currentBook.setId(obj.getString("id"));
                             currentBook.setTitle(obj.getString("title"));
                             currentBook.setAuthorName(authorName);
                             if (authorObj != null) {
                                 currentBook.setAuthorId(authorObj.optString("id", null));
                             }
                             currentBook.setCoverUrl(obj.optString("cover_url"));
                             currentBook.setPremiumOnly(obj.optBoolean("is_premium_only", false));
                            
                            // TÌM FILE MP3 VÀ EPUB TRONG KẾT QUẢ TRẢ VỀ
                            JSONArray filesArray = obj.optJSONArray("book_files");
                            if (filesArray != null) {
                                for (int i = 0; i < filesArray.length(); i++) {
                                    JSONObject file = filesArray.getJSONObject(i);
                                    if (file.optString("file_type").equalsIgnoreCase("mp3") || file.optString("file_type").equalsIgnoreCase("audio")) {
                                        currentBook.setAudioUrl(file.getString("file_url"));
                                    } else if (file.optString("file_type").equalsIgnoreCase("epub")) {
                                        currentBook.setEpubUrl(file.getString("file_url"));
                                    }
                                }
                            }

                            String description = obj.optString("description", "Không có mô tả.");
                            double rating = obj.optDouble("rating_avg", 0.0);
                            int ratingCount = obj.optInt("rating_count", 0);
                            double price = obj.optDouble("price", 0.0);

                            JSONArray libArray = obj.optJSONArray("user_library");
                            if (libArray != null && libArray.length() > 0) {
                                JSONObject libEntry = libArray.getJSONObject(0);
                                isFavorite = libEntry.optBoolean("is_favorite", false);
                                isBookInLibrary = libEntry.optBoolean("is_purchased", false);
                            }

                            runOnUiThread(() -> {
                                if (isBookInLibrary) {
                                    layoutWriteReview.setVisibility(android.view.View.VISIBLE);
                                    // Load user avatar
                                    String userId = sessionManager.getUserId();
                                    if (userId != null) {
                                        String url2 = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=avatar_url";
                                        Request req = new Request.Builder()
                                                .url(url2)
                                                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                                                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                                                .build();
                                        client.newCall(req).enqueue(new Callback() {
                                            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                                            @Override public void onResponse(@NonNull Call call, @NonNull Response r) throws IOException {
                                                if(r.isSuccessful() && r.body()!=null) {
                                                    try {
                                                        JSONArray arr = new JSONArray(r.body().string());
                                                        if(arr.length()>0) {
                                                            String avatar = arr.getJSONObject(0).optString("avatar_url");
                                                            if (avatar != null && !avatar.isEmpty() && !avatar.startsWith("http")) {
                                                                avatar = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatar;
                                                            }
                                                            final String finalAvatar = avatar;
                                                            runOnUiThread(() -> Glide.with(BaseBookDetailActivity.this)
                                                                    .load(finalAvatar)
                                                                    .circleCrop()
                                                                    .placeholder(R.drawable.bacl)
                                                                    .into(ivUserAvatar));
                                                        }
                                                    } catch (Exception e){}
                                                }
                                            }
                                        });
                                    }
                                }
                                updatePurchaseButtonUI();
                            });

                            runOnUiThread(() -> {
                                tvTitle.setText(currentBook.getTitle());
                                tvAuthor.setText(currentBook.getAuthorName());
                                tvDescription.setText(description);
                                rbBook.setRating((float) rating);
                                tvRatingCount.setText(String.format(Locale.getDefault(), "(%d)", ratingCount));
                                
                                tvOverallRating.setText(String.format(Locale.getDefault(), "%.1f", rating));
                                tvOverallRatingCount.setText(String.format(Locale.getDefault(), "/5.0 (%d đánh giá)", ratingCount));
                                Glide.with(BaseBookDetailActivity.this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivRatingBookCover);

                                updateUI(currentBook);
                                onBookLoaded(currentBook);
                                Glide.with(BaseBookDetailActivity.this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivCover);
                                
                                checkLibraryStatus();
                                fetchSimilarBooks(obj.optString("category_id"), bookId);
                                fetchReviews();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(BaseBookDetailActivity.this, "Không tìm thấy dữ liệu sách", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("BookDetail", "Parse error", e);
                        runOnUiThread(() -> Toast.makeText(BaseBookDetailActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(BaseBookDetailActivity.this, "Lỗi kết nối cơ sở dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    protected void fetchChaptersAndPlay() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_chapters?book_id=eq." + bookId + "&order=chapter_index.asc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    PlayerManager.getInstance().playBook(currentBook);
                    startActivity(new Intent(BaseBookDetailActivity.this, AudioPlayerActivity.class));
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        List<Chapter> chapters = new ArrayList<>();
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
                            chapters.add(chapter);
                        }
                        
                        runOnUiThread(() -> {
                            PlayerManager.getInstance().playBook(currentBook);
                            PlayerManager.getInstance().setChapters(chapters);
                            startActivity(new Intent(BaseBookDetailActivity.this, AudioPlayerActivity.class));
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            PlayerManager.getInstance().playBook(currentBook);
                            startActivity(new Intent(BaseBookDetailActivity.this, AudioPlayerActivity.class));
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        PlayerManager.getInstance().playBook(currentBook);
                        startActivity(new Intent(BaseBookDetailActivity.this, AudioPlayerActivity.class));
                    });
                }
            }
        });
    }

    protected void updateUI(Book book) {
        if (isFavorite) {
            btnFavorite.setIconTint(ColorStateList.valueOf(Color.RED));
        } else {
            btnFavorite.setIconTint(ColorStateList.valueOf(Color.GRAY));
        }
    }

    protected void toggleFavorite() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/toggle_favorite_book";
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("user_uuid", userId);
            bodyJson.put("book_uuid", bookId);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(BaseBookDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String result = response.body().string();
                        try {
                            Object jsonResult = new org.json.JSONTokener(result).nextValue();
                            if (jsonResult instanceof Boolean) {
                                isFavorite = (Boolean) jsonResult;
                            } else {
                                isFavorite = result.trim().replace("\"", "").equalsIgnoreCase("true");
                            }
                        } catch (Exception e) {
                            isFavorite = result.trim().replace("\"", "").equalsIgnoreCase("true");
                        }
                        runOnUiThread(() -> {
                            updateUI(currentBook);
                            Toast.makeText(BaseBookDetailActivity.this, isFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    protected void fetchSimilarBooks(String categoryId, String currentBookId) {
        if (categoryId == null || categoryId.isEmpty()) return;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?category_id=eq." + categoryId + 
                     "&id=neq." + currentBookId + "&select=*,authors(name)&limit=5";
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
                        List<Book> similar = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            JSONObject authorObj = obj.optJSONObject("authors");
                            similar.add(new Book(obj.getString("id"), obj.getString("title"), authorObj != null ? authorObj.optString("name") : "Unknown", obj.optString("cover_url"), obj.optBoolean("is_premium_only")));
                        }
                        runOnUiThread(() -> {
                            BookAdapter adapter = new BookAdapter(similar);
                            rvSimilarBooks.setAdapter(adapter);
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    protected void showAuthorBottomSheet() {
        if (currentBook == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, currentBook.getTitle() + " - " + currentBook.getAuthorName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ sách"));
    }

    protected void fetchReviews() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews?book_id=eq." + bookId + "&select=*,profiles(full_name,avatar_url)&order=created_at.desc";
        String token = sessionManager.getAccessToken();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }
        Request request = requestBuilder.get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        List<ReviewAdapter.Review> reviews = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            JSONObject profile = obj.optJSONObject("profiles");
                            String name = profile != null ? profile.optString("full_name", "Ẩn danh") : "Ẩn danh";
                            String avatar = profile != null ? profile.optString("avatar_url", null) : null;
                            if (avatar != null && avatar.equals("null")) {
                                avatar = null;
                            }
                            if (avatar != null && !avatar.isEmpty() && !avatar.startsWith("http")) {
                                avatar = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/avatars/" + avatar;
                            }
                            reviews.add(new ReviewAdapter.Review(
                                    obj.getString("id"), name, avatar, obj.optString("comment"),
                                    obj.optInt("rating", 5), obj.optString("created_at")
                            ));
                        }
                        runOnUiThread(() -> {
                            currentReviewCount = reviews.size();
                            btnViewAllReviews.setText("Xem tất cả " + currentReviewCount + " đánh giá");
                            
                            // Chỉ lấy tối đa 3 đánh giá để hiển thị mặc định
                            List<ReviewAdapter.Review> displayReviews = reviews.size() > 3 ? reviews.subList(0, 3) : reviews;
                            
                            ReviewAdapter adapter = new ReviewAdapter(displayReviews);
                            adapter.setListener(new ReviewAdapter.ReviewInteractionListener() {
                                @Override
                                public void onLikeClick(ReviewAdapter.Review review, int position) {
                                    review.isLiked = !review.isLiked;
                                    review.likeCount += review.isLiked ? 1 : -1;
                                    adapter.notifyItemChanged(position);
                                    Toast.makeText(BaseBookDetailActivity.this, review.isLiked ? "Đã thích" : "Đã bỏ thích", Toast.LENGTH_SHORT).show();
                                }
                            });
                            rvReviews.setAdapter(adapter);
                            
                            // Nếu có hơn 3 đánh giá thì hiện nút Xem tất cả, ngược lại ẩn
                            if (currentReviewCount > 3) {
                                btnViewAllReviews.setVisibility(View.VISIBLE);
                            } else {
                                btnViewAllReviews.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    protected void updatePurchaseButtonUI() {
        if (currentBook == null) return;
        
        if (isBookInLibrary) {
            btnBuy.setEnabled(false);
            btnBuy.setText(currentBook.isPremiumOnly() ? "Đã thêm vào thư viện" : "Đã lưu vào thư viện");
        } else {
            if (currentBook.isPremiumOnly()) {
                if (isUserPremium) {
                    btnBuy.setEnabled(true);
                    btnBuy.setText("Thêm vào thư viện");
                } else {
                    btnBuy.setEnabled(true);
                    btnBuy.setText("Mua Premium");
                }
            } else {
                btnBuy.setEnabled(true);
                btnBuy.setText("Lưu vào thư viện");
            }
        }
        
        if (currentBook.isPremiumOnly() && !isUserPremium) {
            btnRead.setEnabled(false);
            btnPreview.setEnabled(false);
            btnRead.setText("Đọc sách");
            btnPreview.setText("Nghe sách");
            btnRead.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_lock));
            btnPreview.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_lock));
            btnRead.setAlpha(0.5f);
            btnPreview.setAlpha(0.5f);
        } else {
            btnRead.setEnabled(true);
            btnPreview.setEnabled(true);
            btnRead.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_menu_book));
            btnPreview.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_play));
            btnRead.setAlpha(1.0f);
            btnPreview.setAlpha(1.0f);
            
            if (isBookInLibrary) {
                btnRead.setText("Tiếp tục đọc");
                btnPreview.setText("Tiếp tục nghe");
            } else {
                btnRead.setText("Đọc sách");
                btnPreview.setText("Nghe thử");
            }
        }
    }

    protected void performPurchase() {
        if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
            Intent intent = new Intent(this, PremiumActivity.class);
            premiumLauncher.launch(intent);
            return;
        }

        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?on_conflict=user_id,book_id";
        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("user_id", userId);
            bodyJson.put("book_id", bookId);
            bodyJson.put("is_purchased", true);
        } catch (Exception e) {}
        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.parse("application/json"));
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
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(BaseBookDetailActivity.this, currentBook.isPremiumOnly() ? "Đã thêm vào thư viện" : "Đã lưu vào thư viện", Toast.LENGTH_SHORT).show();
                        isBookInLibrary = true;
                        updatePurchaseButtonUI();
                        layoutWriteReview.setVisibility(android.view.View.VISIBLE);
                    } else {
                        Toast.makeText(BaseBookDetailActivity.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    protected void shareBook() {
        if (currentBook == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, currentBook.getTitle() + " - " + currentBook.getAuthorName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ sách"));
    }

    protected void handleDownloadClick() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }

        if (currentBook == null) return;
        if (currentBook.isPremiumOnly() && !isUserPremium) {
            Intent intent = new Intent(this, PremiumActivity.class);
            premiumLauncher.launch(intent);
            return;
        }

        boolean hasAudio = currentBook.getAudioUrl() != null && !currentBook.getAudioUrl().isEmpty();
        boolean hasEbook = currentBook.getEpubUrl() != null && !currentBook.getEpubUrl().isEmpty();

        if (hasAudio && hasEbook) {
            String[] options = {"Tải Sách nói (Audio)", "Tải Sách điện tử (Ebook)"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn định dạng tải xuống")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startDownload("audio", currentBook.getAudioUrl());
                    else startDownload("ebook", currentBook.getEpubUrl());
                })
                .show();
        } else if (hasAudio) {
            startDownload("audio", currentBook.getAudioUrl());
        } else if (hasEbook) {
            startDownload("ebook", currentBook.getEpubUrl());
        } else {
            Toast.makeText(this, "Sách này không có file để tải", Toast.LENGTH_SHORT).show();
        }
    }
}