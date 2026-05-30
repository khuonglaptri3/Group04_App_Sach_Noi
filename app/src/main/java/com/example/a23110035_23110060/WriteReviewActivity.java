package com.example.a23110035_23110060;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WriteReviewActivity extends AppCompatActivity {

    private String bookId;
    private String bookCoverUrl;
    private String bookType;
    private SessionManager sessionManager;
    private OkHttpClient client = new OkHttpClient();

    private ImageView ivBookCover;
    private TextView tvReviewType;
    private RatingBar rbReview;
    private ChipGroup chipGroupTags;
    private EditText etComment;
    private MaterialButton btnSubmit, btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        sessionManager = new SessionManager(this);
        bookId = getIntent().getStringExtra("bookId");
        bookCoverUrl = getIntent().getStringExtra("bookCoverUrl");
        bookType = getIntent().getStringExtra("bookType");

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBookCover = findViewById(R.id.iv_book_cover);
        tvReviewType = findViewById(R.id.tv_review_type);
        rbReview = findViewById(R.id.rb_review);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        etComment = findViewById(R.id.et_comment);
        btnSubmit = findViewById(R.id.btn_submit);
        btnClose = findViewById(R.id.btn_close);

        if (bookCoverUrl != null && !bookCoverUrl.isEmpty()) {
            Glide.with(this).load(bookCoverUrl).placeholder(R.drawable.bacl).into(ivBookCover);
        }

        if ("audio".equalsIgnoreCase(bookType)) {
            tvReviewType.setText("Đánh giá Sách nói");
        } else {
            tvReviewType.setText("Đánh giá Ebook");
        }
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        rbReview.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating > 0) {
                btnSubmit.setEnabled(true);
                btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0288D1")));
                // Update star colors
                ratingBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // amber-500
            } else {
                btnSubmit.setEnabled(false);
                btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                ratingBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            }
        });

        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            chip.setOnClickListener(v -> {
                String currentText = etComment.getText().toString();
                String chipText = chip.getText().toString();
                if (currentText.isEmpty()) {
                    etComment.setText(chipText);
                } else {
                    etComment.setText(currentText + ", " + chipText);
                }
                etComment.setSelection(etComment.getText().length());
            });
        }

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        int rating = (int) rbReview.getRating();
        String comment = etComment.getText().toString().trim();

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("rating", rating);
            json.put("comment", comment);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(WriteReviewActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Gửi đánh giá");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(WriteReviewActivity.this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(WriteReviewActivity.this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Gửi đánh giá");
                    }
                });
            }
        });
    }
}
