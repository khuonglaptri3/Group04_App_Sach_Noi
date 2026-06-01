package com.example.a23110035_23110060.data.repository;

import com.example.a23110035_23110060.model.Book;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.data.local.BookDao;
import com.example.a23110035_23110060.data.local.BookEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookRepository {
    private final BookDao bookDao;
    private final OkHttpClient client;
    private final ExecutorService executorService;

    public BookRepository(BookDao bookDao, OkHttpClient client) {
        this.bookDao = bookDao;
        this.client = client;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<BookEntity>> getFeaturedBooks(String type) {
        //fff Lấy dữ liệu ngay lập tức từ Room (Local)
        LiveData<List<BookEntity>> localData = bookDao.getFeaturedBooks();
        
        // Đồng thời gọi API ngầm để cập nhật dữ liệu mới nhất từ Supabase
        refreshFeaturedBooksFromNetwork(type);
        
        return localData;
    }

    private void refreshFeaturedBooksFromNetwork(String type) {
        String filter = type.equals("audio") ? "is_audiobook=eq.true" : "is_ebook=eq.true";
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?select=*,authors(name)&" + filter + "&limit=10";


        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("BookRepository", "Network fetch failed", e);
                // Dù lỗi mạng, dữ liệu ở Room vẫn còn nên UI không bị trắng
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<BookEntity> newBooks = new ArrayList<>();
                        long currentTime = System.currentTimeMillis();
                        
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            JSONObject authorObj = obj.optJSONObject("authors");
                            String authorName = authorObj != null ? authorObj.optString("name") : "Unknown";
                            
                            BookEntity entity = new BookEntity(obj.getString("id"));
                            entity.title = obj.getString("title");
                            entity.authorName = authorName;
                            entity.coverUrl = obj.optString("cover_url");
                            entity.isFeatured = true;
                            entity.cachedAt = currentTime;
                            newBooks.add(entity);
                        }
                        
                        // Ghi đè dữ liệu mới vào Room DB
                        executorService.execute(() -> {
                            bookDao.clearFeaturedBooks();
                            bookDao.insertBooks(newBooks);
                        });
                        
                    } catch (Exception e) {
                        Log.e("BookRepository", "Parsing error", e);
                    }
                }
            }
        });
    }
}
