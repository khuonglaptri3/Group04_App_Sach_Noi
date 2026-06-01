package com.example.a23110035_23110060.view.fragment;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.adapter.SectionAdapter;

import com.example.a23110035_23110060.model.FeaturedReview;
import com.example.a23110035_23110060.model.Section;
import com.example.a23110035_23110060.controller.NetworkClient;
import com.example.a23110035_23110060.model.Category;
import com.example.a23110035_23110060.model.Book;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
import androidx.lifecycle.ViewModelProvider;
import com.example.a23110035_23110060.ui.viewmodel.HomeViewModel;
import com.example.a23110035_23110060.data.local.BookEntity;

public class HomeTabFragment extends Fragment {

    private RecyclerView rvSections;
    private SwipeRefreshLayout swipeRefresh;
    private OkHttpClient client;
    private List<Section> sections = new ArrayList<>();
    private SectionAdapter adapter;
    private HomeViewModel viewModel;

    public static HomeTabFragment newInstance(String type) {
        HomeTabFragment fragment = new HomeTabFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        client = NetworkClient.getClient(requireContext());
        rvSections = view.findViewById(R.id.rvSections);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        
        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SectionAdapter(sections);
        rvSections.setAdapter(adapter);
        
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        String type = getArguments() != null ? getArguments().getString("type") : "audio";
        
        swipeRefresh.setOnRefreshListener(() -> fetchDatabaseData(type));
        
        fetchDatabaseData(type);
    }

    private void fetchDatabaseData(String type) {
        swipeRefresh.setRefreshing(true);
        viewModel.getFeaturedBooks(type).observe(getViewLifecycleOwner(), bookEntities -> {
            if (bookEntities != null && !bookEntities.isEmpty()) {
                List<Book> books = new ArrayList<>();
                for (BookEntity entity : bookEntities) {
                    books.add(new Book(
                        entity.id,
                        entity.title,
                        entity.authorName,
                        entity.coverUrl,
                        false
                    ));
                }
                fetchCategories(books);
            } else {
                // If it's empty, it might still be fetching from network
                // We'll just stop the refreshing indicator for now after a short delay
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
                }
            }
        });
    }

    private void fetchCategories(List<Book> books) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/categories?select=*&limit=10";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchFeaturedReviews(books, new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<Category> categories = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Category cat = new Category(
                                obj.getString("id"),
                                obj.getString("name_vi"),
                                obj.optString("name_en"),
                                obj.optString("icon_url")
                            );
                            // Assign a color for the chip background (legacy support)
                            // We can use a list of colors or just one for now
                            categories.add(cat);
                        }
                    } catch (Exception e) {}
                }
                fetchFeaturedReviews(books, categories);
            }
        });
    }

    private void fetchFeaturedReviews(List<Book> books, List<Category> categories) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews?select=*,profiles(full_name,avatar_url),books(id,title,cover_url,rating_avg,is_audiobook,authors(name))&order=rating.desc&limit=5";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                updateUI(books, categories, new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<FeaturedReview> featuredReviews = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            JSONObject profile = obj.optJSONObject("profiles");
                            JSONObject bookObj = obj.optJSONObject("books");
                            JSONObject authorObj = bookObj != null ? bookObj.optJSONObject("authors") : null;

                            String reviewerName = profile != null ? profile.optString("full_name", "Ẩn danh") : "Ẩn danh";
                            String avatarUrl = profile != null ? profile.optString("avatar_url") : null;
                            
                            String bookId = bookObj != null ? bookObj.optString("id") : "";
                            String bookTitle = bookObj != null ? bookObj.optString("title") : "";
                            String bookCoverUrl = bookObj != null ? bookObj.optString("cover_url") : "";
                            double bookRating = bookObj != null ? bookObj.optDouble("rating_avg", 0.0) : 0.0;
                            boolean isAudiobook = bookObj != null && bookObj.optBoolean("is_audiobook", false);
                            String bookType = isAudiobook ? "SÁCH NÓI" : "EBOOK";
                            String bookAuthor = authorObj != null ? authorObj.optString("name") : "Unknown";

                            FeaturedReview fr = new FeaturedReview(
                                    obj.getString("id"), reviewerName, avatarUrl,
                                    obj.optString("comment"), obj.optInt("rating", 5), obj.optString("created_at"),
                                    bookId, bookTitle, bookAuthor, bookCoverUrl, bookType, bookRating
                            );
                            featuredReviews.add(fr);
                        }
                    } catch (Exception e) {
                        Log.e("HomeTab", "Error parsing reviews", e);
                    }
                }
                updateUI(books, categories, featuredReviews);
            }
        });
    }

    private void updateUI(List<Book> books, List<Category> categories, List<FeaturedReview> featuredReviews) {
        String type = getArguments() != null ? getArguments().getString("type") : "audio";
        String filter = type.equals("audio") ? "is_audiobook=eq.true" : "is_ebook=eq.true";
        
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                sections.clear();
                if (!books.isEmpty()) {
                    sections.add(new Section("THỊNH HÀNH HÔM NAY", books, Section.TYPE_CAROUSEL, filter));
                }
                if (!categories.isEmpty()) {
                    sections.add(new Section("DANH MỤC", categories, Section.TYPE_CATEGORIES, true));
                }
                if (!featuredReviews.isEmpty()) {
                    sections.add(new Section("ĐÁNH GIÁ NỔI BẬT", featuredReviews, Section.TYPE_FEATURED_REVIEWS, 0));
                }
                if (!books.isEmpty()) {
                    // Create a reversed copy to simulate a different list for 'Mới xuất bản'
                    List<Book> newBooks = new ArrayList<>(books);
                    java.util.Collections.reverse(newBooks);
                    sections.add(new Section("MỚI XUẤT BẢN", newBooks, Section.TYPE_CAROUSEL, filter));
                }
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
            });
        }
    }
}
