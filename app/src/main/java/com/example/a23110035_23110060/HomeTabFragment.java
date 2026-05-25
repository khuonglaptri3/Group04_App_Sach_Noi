package com.example.a23110035_23110060;

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

public class HomeTabFragment extends Fragment {

    private RecyclerView rvSections;
    private SwipeRefreshLayout swipeRefresh;
    private OkHttpClient client = new OkHttpClient();
    private List<Section> sections = new ArrayList<>();
    private SectionAdapter adapter;

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
        
        rvSections = view.findViewById(R.id.rvSections);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        
        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SectionAdapter(sections);
        rvSections.setAdapter(adapter);
        
        String type = getArguments() != null ? getArguments().getString("type") : "audio";
        
        swipeRefresh.setOnRefreshListener(() -> fetchDatabaseData(type));
        
        fetchDatabaseData(type);
    }

    private void fetchDatabaseData(String type) {
        swipeRefresh.setRefreshing(true);
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
        
        String filter = type.equals("audio") ? "is_audiobook=eq.true" : "is_ebook=eq.true";
        String url = supabaseUrl + "/rest/v1/books?select=*,authors(name)&" + filter + "&limit=10";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Book> books = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            JSONObject authorObj = obj.optJSONObject("authors");
                            String authorName = authorObj != null ? authorObj.optString("name") : "Unknown";
                            
                            books.add(new Book(
                                obj.getString("id"),
                                obj.getString("title"),
                                authorName,
                                obj.optString("cover_url"),
                                obj.optBoolean("is_premium_only")
                            ));
                        }
                        fetchCategories(books);
                    } catch (Exception e) {
                        Log.e("HomeTab", "Parsing error", e);
                        if (isAdded()) requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
                    }
                } else {
                    if (isAdded()) requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
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
                updateUI(books, new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<Category> categories = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            categories.add(new Category(
                                obj.getString("name_vi"),
                                obj.optString("icon_url"),
                                Color.parseColor("#4f378a")
                            ));
                        }
                    } catch (Exception e) {}
                }
                updateUI(books, categories);
            }
        });
    }

    private void updateUI(List<Book> books, List<Category> categories) {
        String type = getArguments() != null ? getArguments().getString("type") : "audio";
        String filter = type.equals("audio") ? "is_audiobook=eq.true" : "is_ebook=eq.true";
        
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                sections.clear();
                if (!books.isEmpty()) {
                    sections.add(new Section("THỊNH HÀNH HÔM NAY", books, Section.TYPE_CAROUSEL, filter));
                }
                sections.add(new Section("BANNER", (List<Book>)null, Section.TYPE_BANNER));
                if (!categories.isEmpty()) {
                    sections.add(new Section("DANH MỤC", categories, Section.TYPE_CATEGORIES, true));
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
