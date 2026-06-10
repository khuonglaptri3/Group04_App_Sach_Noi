package com.example.a23110035_23110060.view.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;
import com.example.a23110035_23110060.controller.SessionManager;
import com.example.a23110035_23110060.view.adapter.ReviewAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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

public class BookReviewsBottomSheet extends BottomSheetDialogFragment {

    private String bookId;
    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<ReviewAdapter.Review> reviewList = new ArrayList<>();
    private LinearProgressIndicator progressIndicator;

    public BookReviewsBottomSheet() {}

    public static BookReviewsBottomSheet newInstance(String bookId) {
        BookReviewsBottomSheet fragment = new BookReviewsBottomSheet();
        Bundle args = new Bundle();
        args.putString("bookId", bookId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString("bookId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_reviews, container, false);
        
        progressIndicator = view.findViewById(R.id.progress_indicator);
        rvReviews = view.findViewById(R.id.rv_reviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(adapter);

        fetchReviews();
        
        return view;
    }

    private void fetchReviews() {
        if (bookId == null) return;
        
        progressIndicator.setVisibility(View.VISIBLE);
        
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews?book_id=eq." + bookId + 
                     "&select=*,profiles(full_name,avatar_url)&order=rating.desc&limit=10";
                     
        SessionManager sessionManager = new SessionManager(requireContext());
        String token = sessionManager.getAccessToken();
        
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
                
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        new OkHttpClient().newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
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
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                progressIndicator.setVisibility(View.GONE);
                            });
                        }
                    } catch (Exception e) {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
                    }
                } else {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
                }
            }
        });
    }
}
