package com.example.a23110035_23110060;

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

public class BookDetailActivity extends AppCompatActivity {

    private String bookId;
    private Book currentBook;
    private OkHttpClient client = new OkHttpClient();
    private SessionManager sessionManager;

    private ImageView ivCover;
    private TextView tvTitle, tvAuthor, tvRatingCount, tvDescription, btnReadMore;
    private RatingBar rbBook;
    private MaterialButton btnBuy, btnPreview, btnRead, btnFavorite;
    private RecyclerView rvSimilarBooks;
    
    private boolean isDescriptionExpanded = false;
    private boolean isFavorite = false;
    private boolean isBookInLibrary = false;
    private boolean isUserPremium = false;
    private View layoutWriteReview;
    private RecyclerView rvReviews;
    private TextView tvOverallRating, tvOverallRatingCount;
    private ImageView ivRatingBookCover, ivUserAvatar;
    private MaterialButton btnViewAllReviews;
    private int currentReviewCount = 0;

    private final ActivityResultLauncher<Intent> writeReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchReviews();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        sessionManager = new SessionManager(this);
        bookId = getIntent().getStringExtra("bookId");

        initViews();
        setupListeners();
        fetchBookDetails();
    }

    private void initViews() {
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

    private void setupListeners() {
        findViewById(R.id.btn_share).setOnClickListener(v -> shareBook());
        
        btnReadMore.setOnClickListener(v -> {
            isDescriptionExpanded = !isDescriptionExpanded;
            tvDescription.setMaxLines(isDescriptionExpanded ? Integer.MAX_VALUE : 3);
            btnReadMore.setText(isDescriptionExpanded ? "Thu gọn" : "Xem thêm");
        });

        btnPreview.setOnClickListener(v -> {
            if (currentBook != null && currentBook.getAudioUrl() != null) {
                fetchChaptersAndPlay();
            } else {
                Toast.makeText(this, "Sách này chưa có file âm thanh trong database", Toast.LENGTH_SHORT).show();
            }
        });

        btnBuy.setOnClickListener(v -> performPurchase());

        btnRead.setOnClickListener(v -> {
            Intent intent = new Intent(this, EbookReaderActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        findViewById(R.id.btn_write_review).setOnClickListener(v -> writeReview());
        
        btnViewAllReviews.setOnClickListener(v -> {
            Toast.makeText(this, "Xem tất cả đánh giá", Toast.LENGTH_SHORT).show();
        });
    }

    private void writeReview() {
        if (currentBook == null) return;
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("bookId", bookId);
        intent.putExtra("bookCoverUrl", currentBook.getCoverUrl());
        intent.putExtra("bookType", currentBook.isAudiobook() ? "audio" : "ebook");
        writeReviewLauncher.launch(intent);
    }

    // submitReview removed as it is now handled by WriteReviewActivity

    private void checkUserPremiumStatus() {
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

    private void fetchBookDetails() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        // CẬP NHẬT TRUY VẤN: Lấy luôn cả book_files trong 1 lần gọi
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?id=eq." + bookId + 
                     "&select=*,authors(name),categories(name_vi),user_library(is_favorite,is_purchased),book_files(file_url,file_type)";
        
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
                            String authorName = authorObj != null ? authorObj.optString("name") : "Unknown";
                            
                            currentBook = new Book();
                            currentBook.setId(obj.getString("id"));
                            currentBook.setTitle(obj.getString("title"));
                            currentBook.setAuthorName(authorName);
                            currentBook.setCoverUrl(obj.optString("cover_url"));
                            
                            // TÌM FILE MP3 TRONG KẾT QUẢ TRẢ VỀ
                            JSONArray filesArray = obj.optJSONArray("book_files");
                            if (filesArray != null) {
                                for (int i = 0; i < filesArray.length(); i++) {
                                    JSONObject file = filesArray.getJSONObject(i);
                                    if (file.optString("file_type").equalsIgnoreCase("mp3")) {
                                        currentBook.setAudioUrl(file.getString("file_url"));
                                        break;
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
                                        Request req = new Request.Builder().url(url2).addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY).build();
                                        client.newCall(req).enqueue(new Callback() {
                                            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                                            @Override public void onResponse(@NonNull Call call, @NonNull Response r) throws IOException {
                                                if(r.isSuccessful() && r.body()!=null) {
                                                    try {
                                                        JSONArray arr = new JSONArray(r.body().string());
                                                        if(arr.length()>0) {
                                                            String avatar = arr.getJSONObject(0).optString("avatar_url");
                                                            runOnUiThread(() -> Glide.with(BookDetailActivity.this).load(avatar).placeholder(R.drawable.bacl).into(ivUserAvatar));
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
                                Glide.with(BookDetailActivity.this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivRatingBookCover);

                                updateFavoriteUI();
                                Glide.with(BookDetailActivity.this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivCover);
                                
                                checkUserPremiumStatus();
                                fetchSimilarBooks(obj.optString("category_id"));
                                fetchReviews();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("BookDetail", "Parse error", e);
                    }
                }
            }
        });
    }

    private void fetchChaptersAndPlay() {
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
                    startActivity(new Intent(BookDetailActivity.this, AudioPlayerActivity.class));
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
                            startActivity(new Intent(BookDetailActivity.this, AudioPlayerActivity.class));
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            PlayerManager.getInstance().playBook(currentBook);
                            startActivity(new Intent(BookDetailActivity.this, AudioPlayerActivity.class));
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        PlayerManager.getInstance().playBook(currentBook);
                        startActivity(new Intent(BookDetailActivity.this, AudioPlayerActivity.class));
                    });
                }
            }
        });
    }

    private void updateFavoriteUI() {
        if (isFavorite) {
            btnFavorite.setIconTint(ColorStateList.valueOf(Color.RED));
        } else {
            btnFavorite.setIconTint(ColorStateList.valueOf(Color.GRAY));
        }
    }

    private void toggleFavorite() {
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
                runOnUiThread(() -> Toast.makeText(BookDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show());
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
                            updateFavoriteUI();
                            Toast.makeText(BookDetailActivity.this, isFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void fetchSimilarBooks(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?category_id=eq." + categoryId + 
                     "&id=neq." + bookId + "&select=*,authors(name)&limit=5";
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

    private void shareBook() {
        if (currentBook == null) return;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, currentBook.getTitle() + " - " + currentBook.getAuthorName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ sách"));
    }

    private void fetchReviews() {
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
                            String avatar = profile != null ? profile.optString("avatar_url") : null;
                            reviews.add(new ReviewAdapter.Review(
                                    obj.getString("id"), name, avatar, obj.optString("comment"),
                                    obj.optInt("rating", 5), obj.optString("created_at")
                            ));
                        }
                        runOnUiThread(() -> {
                            currentReviewCount = reviews.size();
                            btnViewAllReviews.setText("Xem tất cả " + currentReviewCount + " đánh giá");
                            ReviewAdapter adapter = new ReviewAdapter(reviews);
                            rvReviews.setAdapter(adapter);
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void updatePurchaseButtonUI() {
        if (currentBook == null) return;
        
        if (isBookInLibrary) {
            btnBuy.setEnabled(false);
            btnBuy.setText(currentBook.isPremiumOnly() ? "Đã thêm vào thư viện" : "Đã lưu vào thư viện");
            return;
        }

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

    private void performPurchase() {
        if (currentBook != null && currentBook.isPremiumOnly() && !isUserPremium) {
            Toast.makeText(this, "Vui lòng mua gói Premium để truy cập sách này!", Toast.LENGTH_SHORT).show();
            // Intent to Premium Activity here if it exists
            return;
        }

        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library";
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
                        Toast.makeText(BookDetailActivity.this, currentBook.isPremiumOnly() ? "Đã thêm vào thư viện" : "Đã lưu vào thư viện", Toast.LENGTH_SHORT).show();
                        isBookInLibrary = true;
                        updatePurchaseButtonUI();
                        layoutWriteReview.setVisibility(android.view.View.VISIBLE);
                    } else {
                        Toast.makeText(BookDetailActivity.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}