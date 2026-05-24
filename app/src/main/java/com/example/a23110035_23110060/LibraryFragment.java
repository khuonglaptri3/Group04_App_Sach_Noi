package com.example.a23110035_23110060;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
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

public class LibraryFragment extends Fragment {

    private RecyclerView rvLibraryBooks;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private OkHttpClient client = new OkHttpClient();
    private List<LibraryBook> libraryBooks = new ArrayList<>();
    private LibraryBookAdapter adapter;
    private SessionManager sessionManager;
    private String currentFilter = "recent";
    private MaterialButton btnRecent, btnPurchased, btnDownloaded, btnFavorite;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        rvLibraryBooks = view.findViewById(R.id.rvLibraryBooks);
        swipeRefresh = view.findViewById(R.id.swipeRefreshLibrary);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        rvLibraryBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LibraryBookAdapter(libraryBooks);
        rvLibraryBooks.setAdapter(adapter);

        setupFilters(view);

        swipeRefresh.setOnRefreshListener(() -> fetchLibraryData(currentFilter));
        fetchLibraryData("recent");

        setupMiniPlayer(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncMiniPlayer();
        startProgressUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopProgressUpdate();
    }

    private void syncMiniPlayer() {
        View miniPlayer = getView() != null ? getView().findViewById(R.id.miniPlayer) : null;
        if (miniPlayer != null) {
            Book currentBook = PlayerManager.getInstance().getCurrentBook();
            if (currentBook != null) {
                miniPlayer.setVisibility(View.VISIBLE);
                ((TextView) miniPlayer.findViewById(R.id.tvMiniTitle)).setText(currentBook.getTitle());
                ((TextView) miniPlayer.findViewById(R.id.tvMiniAuthor)).setText(currentBook.getAuthorName());
                com.google.android.material.button.MaterialButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                btnPlay.setIconResource(PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            } else {
                miniPlayer.setVisibility(View.GONE);
            }
        }
    }

    private void startProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                View miniPlayer = getView() != null ? getView().findViewById(R.id.miniPlayer) : null;
                if (miniPlayer != null && PlayerManager.getInstance().isPlaying()) {
                    LinearProgressIndicator progressIndicator = miniPlayer.findViewById(R.id.miniPlayerProgress);
                    int current = PlayerManager.getInstance().getCurrentPosition();
                    int total = PlayerManager.getInstance().getDuration();
                    if (total > 0) {
                        progressIndicator.setMax(total);
                        progressIndicator.setProgress(current);
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.postDelayed(progressRunnable, 1000);
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
    }

    private void setupMiniPlayer(View view) {
        View miniPlayer = view.findViewById(R.id.miniPlayer);
        if (miniPlayer != null) {
            miniPlayer.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AudioPlayerActivity.class);
                startActivity(intent);
            });
            miniPlayer.findViewById(R.id.btnMiniPlayPause).setOnClickListener(v -> {
                PlayerManager.getInstance().togglePlayPause();
                syncMiniPlayer();
            });
            miniPlayer.findViewById(R.id.btnMiniNext).setOnClickListener(v -> {
                PlayerManager.getInstance().skipNext();
                syncMiniPlayer();
            });
        }
    }

    private void setupFilters(View view) {
        btnRecent = view.findViewById(R.id.btnFilterRecent);
        btnPurchased = view.findViewById(R.id.btnFilterPurchased);
        btnDownloaded = view.findViewById(R.id.btnFilterDownloaded);
        btnFavorite = view.findViewById(R.id.btnFilterFavorite);

        View.OnClickListener listener = v -> {
            updateFilterUI((MaterialButton) v);
            if (v.getId() == R.id.btnFilterRecent) currentFilter = "recent";
            else if (v.getId() == R.id.btnFilterPurchased) currentFilter = "is_purchased=eq.true";
            else if (v.getId() == R.id.btnFilterDownloaded) currentFilter = "is_downloaded=eq.true";
            else if (v.getId() == R.id.btnFilterFavorite) currentFilter = "is_favorite=eq.true";
            fetchLibraryData(currentFilter);
        };

        btnRecent.setOnClickListener(listener);
        btnPurchased.setOnClickListener(listener);
        btnDownloaded.setOnClickListener(listener);
        btnFavorite.setOnClickListener(listener);
        updateFilterUI(btnRecent);
    }

    private void updateFilterUI(MaterialButton selected) {
        MaterialButton[] buttons = {btnRecent, btnPurchased, btnDownloaded, btnFavorite};
        int primaryColor = Color.parseColor("#4f378a");
        for (MaterialButton btn : buttons) {
            if (btn == selected) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                btn.setTextColor(Color.WHITE);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor));
            } else {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
                btn.setTextColor(Color.WHITE);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btn.setStrokeWidth(2);
            }
        }
    }

    private void fetchLibraryData(String filter) {
        swipeRefresh.setRefreshing(true);
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        String filterQuery = filter.equals("recent") ? "" : "&" + filter;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + 
                     "&select=*,books(id,title,cover_url,is_premium_only,authors(name))" +
                     (filter.equals("recent") ? "&order=last_accessed.desc" : "") + filterQuery;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> swipeRefresh.setRefreshing(false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        libraryBooks.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            JSONObject bookObj = item.getJSONObject("books");
                            JSONObject authorObj = bookObj.optJSONObject("authors");
                            LibraryBook lb = new LibraryBook();
                            lb.setBookId(bookObj.getString("id"));
                            lb.setTitle(bookObj.getString("title"));
                            lb.setAuthor(authorObj != null ? authorObj.optString("name") : "Unknown");
                            lb.setCoverUrl(bookObj.optString("cover_url"));
                            lb.setPremium(bookObj.optBoolean("is_premium_only"));
                            lb.setProgress((int)(Math.random() * 100));
                            lb.setTimeRemaining("Còn " + (int)(Math.random() * 10) + "g");
                            libraryBooks.add(lb);
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                swipeRefresh.setRefreshing(false);
                                layoutEmpty.setVisibility(libraryBooks.isEmpty() ? View.VISIBLE : View.GONE);
                                rvLibraryBooks.setVisibility(libraryBooks.isEmpty() ? View.GONE : View.VISIBLE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Library", "Error", e);
                    }
                }
            }
        });
    }
}