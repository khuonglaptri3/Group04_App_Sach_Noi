package com.example.a23110035_23110060.view.activity;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.adapter.ReviewAdapter;

import com.example.a23110035_23110060.controller.SessionManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllReviewsActivity extends AppCompatActivity {

    private RecyclerView rvAllReviews;
    private ReviewAdapter adapter;
    private final List<ReviewAdapter.Review> reviewList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private String bookId;
    private LinearProgressIndicator progressIndicator;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        sessionManager = new SessionManager(this);
        bookId = getIntent().getStringExtra("bookId");
        String bookTitle = getIntent().getStringExtra("bookTitle");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (bookTitle != null) {
            toolbar.setSubtitle(bookTitle);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressIndicator = findViewById(R.id.progress_indicator);
        rvAllReviews = findViewById(R.id.rv_all_reviews);
        rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(reviewList);
        rvAllReviews.setAdapter(adapter);

        fetchReviews();
    }

    private void fetchReviews() {
        if (bookId == null) return;
        
        progressIndicator.setVisibility(View.VISIBLE);
        
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews?book_id=eq." + bookId + 
                     "&select=*,profiles(full_name,avatar_url)&order=created_at.desc";
        
        String token = sessionManager.getAccessToken();
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
        
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        reviewList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            JSONObject profile = obj.optJSONObject("profiles");
                            String name = profile != null ? profile.optString("full_name", "Ẩn danh") : "Ẩn danh";
                            String avatar = profile != null ? profile.optString("avatar_url") : null;
                            reviewList.add(new ReviewAdapter.Review(
                                    obj.getString("id"), name, avatar, obj.optString("comment"),
                                    obj.optInt("rating", 5), obj.optString("created_at")
                            ));
                        }
                        runOnUiThread(() -> {
                            adapter.setListener((review, position1) -> {
                                review.isLiked = !review.isLiked;
                                review.likeCount += review.isLiked ? 1 : -1;
                                adapter.notifyItemChanged(position1);
                            });
                            adapter.notifyDataSetChanged();
                            progressIndicator.setVisibility(View.GONE);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
                    }
                } else {
                    runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
                }
            }
        });
    }
}