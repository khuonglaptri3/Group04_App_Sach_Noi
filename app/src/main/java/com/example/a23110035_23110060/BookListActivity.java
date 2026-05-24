package com.example.a23110035_23110060;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
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

public class BookListActivity extends AppCompatActivity {

    private RecyclerView rvBookList;
    private BookAdapter adapter;
    private List<Book> bookList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        String title = getIntent().getStringExtra("title");
        String filter = getIntent().getStringExtra("filter");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvBookList = findViewById(R.id.rvBookList);
        rvBookList.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new BookAdapter(bookList);
        rvBookList.setAdapter(adapter);

        fetchBooks(filter);
    }

    private void fetchBooks(String filter) {
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
        
        String queryFilter = (filter != null && !filter.isEmpty()) ? filter : "is_audiobook=eq.true";
        String url = supabaseUrl + "/rest/v1/books?select=*,authors(name)&" + queryFilter + "&limit=100";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("BookList", "Fetch failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        bookList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            JSONObject authorObj = obj.optJSONObject("authors");
                            String authorName = authorObj != null ? authorObj.optString("name") : "Unknown";
                            
                            bookList.add(new Book(
                                obj.getString("id"),
                                obj.getString("title"),
                                authorName,
                                obj.optString("cover_url"),
                                obj.optBoolean("is_premium_only")
                            ));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        Log.e("BookList", "Parsing error", e);
                    }
                }
            }
        });
    }
}