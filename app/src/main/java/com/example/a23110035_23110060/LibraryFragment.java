package com.example.a23110035_23110060;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
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
    private String currentTabFilter = "recent";
    private String currentFormatFilter = "all";
    private com.google.android.material.tabs.TabLayout tabLayoutLibrary;
    private com.google.android.material.chip.ChipGroup chipGroupFormat;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private ObjectAnimator rotateAnimator;

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
        adapter = new LibraryBookAdapter(libraryBooks, (book, position) -> {
            deleteBookFromLibrary(book, position);
        });
        rvLibraryBooks.setAdapter(adapter);

        setupFilters(view);

        swipeRefresh.setOnRefreshListener(() -> fetchLibraryData());
        fetchLibraryData();

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
                
                ImageView ivCover = miniPlayer.findViewById(R.id.ivMiniCover);
                Glide.with(this).load(currentBook.getCoverUrl()).placeholder(R.drawable.bacl).into(ivCover);

                com.google.android.material.button.MaterialButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                boolean isPlaying = PlayerManager.getInstance().isPlaying();
                btnPlay.setIconResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

                handleRotation(ivCover, isPlaying);
            } else {
                miniPlayer.setVisibility(View.GONE);
            }
        }
    }

    private void handleRotation(View view, boolean isPlaying) {
        if (rotateAnimator == null) {
            rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
            rotateAnimator.setDuration(10000);
            rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnimator.setInterpolator(new LinearInterpolator());
            rotateAnimator.start();
        }
        if (isPlaying) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (rotateAnimator.isPaused()) rotateAnimator.resume();
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                rotateAnimator.pause();
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
        tabLayoutLibrary = view.findViewById(R.id.tabLayoutLibrary);
        chipGroupFormat = view.findViewById(R.id.chipGroupFormat);

        tabLayoutLibrary.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentTabFilter = "recent"; break;
                    case 1: currentTabFilter = "is_purchased=eq.true"; break;
                    case 2: currentTabFilter = "is_favorite=eq.true"; break;
                    case 3: currentTabFilter = "is_downloaded=eq.true"; break;
                }
                fetchLibraryData();
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        chipGroupFormat.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll) currentFormatFilter = "all";
            else if (id == R.id.chipAudiobook) currentFormatFilter = "audiobook";
            else if (id == R.id.chipEbook) currentFormatFilter = "ebook";
            fetchLibraryData();
        });
    }

    private void fetchLibraryData() {
        swipeRefresh.setRefreshing(true);
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        String filterQuery = currentTabFilter.equals("recent") ? "" : "&" + currentTabFilter;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + 
                     "&select=*,books(id,title,cover_url,is_premium_only,is_audiobook,is_ebook,authors(name))" +
                     (currentTabFilter.equals("recent") ? "&order=last_accessed.desc" : "") + filterQuery;

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
                            
                            boolean isAudiobook = bookObj.optBoolean("is_audiobook", false);
                            boolean isEbook = bookObj.optBoolean("is_ebook", false);
                            
                            if (currentFormatFilter.equals("audiobook") && !isAudiobook) continue;
                            if (currentFormatFilter.equals("ebook") && !isEbook) continue;

                            JSONObject authorObj = bookObj.optJSONObject("authors");
                            LibraryBook lb = new LibraryBook();
                            lb.setBookId(bookObj.getString("id"));
                            lb.setTitle(bookObj.getString("title"));
                            lb.setAuthor(authorObj != null ? authorObj.optString("name") : "Unknown");
                            lb.setCoverUrl(bookObj.optString("cover_url"));
                            lb.setPremium(bookObj.optBoolean("is_premium_only"));
                            lb.setProgress(item.optInt("progress", 0));
                            int timeRemaining = item.optInt("time_remaining_minutes", 0);
                            lb.setTimeRemaining(timeRemaining > 0 ? "Còn " + (timeRemaining/60) + "g" : "");
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


    private void deleteBookFromLibrary(LibraryBook book, int position) {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/user_library?user_id=eq." + userId + "&book_id=eq." + book.getBookId();
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            libraryBooks.remove(position);
                            adapter.notifyItemRemoved(position);
                            if (libraryBooks.isEmpty()) {
                                layoutEmpty.setVisibility(View.VISIBLE);
                                rvLibraryBooks.setVisibility(View.GONE);
                            }
                            Toast.makeText(getContext(), "Đã xóa khỏi thư viện", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }
}

