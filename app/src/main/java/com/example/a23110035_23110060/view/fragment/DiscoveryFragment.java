package com.example.a23110035_23110060.view.fragment;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.BookListActivity;
import com.example.a23110035_23110060.view.adapter.SuggestionAdapter;
import com.example.a23110035_23110060.view.adapter.DiscoveryCategoryAdapter;
import com.example.a23110035_23110060.view.activity.AudioPlayerActivity;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.model.Category;
import com.example.a23110035_23110060.controller.NetworkClient;
import com.example.a23110035_23110060.controller.PlayerManager;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DiscoveryFragment extends Fragment {

    private RecyclerView rvDiscoveryCategories, rvSuggestions;
    private EditText etSearch;
    private ImageView btnClearSearch, ivSearchAction;
    private MaterialCardView cardSuggestions, cardFeaturedCollection;
    private OkHttpClient client;
    private final List<Book> suggestionList = new ArrayList<>();
    private SuggestionAdapter suggestionAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable, progressRunnable;
    private ObjectAnimator rotateAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = NetworkClient.getClient(requireContext());
        rvDiscoveryCategories = view.findViewById(R.id.rvDiscoveryCategories);
        rvSuggestions = view.findViewById(R.id.rvSuggestions);
        etSearch = view.findViewById(R.id.etSearch);
        ivSearchAction = view.findViewById(R.id.ivSearchAction);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        cardSuggestions = view.findViewById(R.id.cardSuggestions);
        cardFeaturedCollection = view.findViewById(R.id.cardFeaturedCollection);

        // 1. Categories Grid
        rvDiscoveryCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        loadCategories();

        // 2. Suggestions RecyclerView (Dọc)
        suggestionAdapter = new SuggestionAdapter(suggestionList);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(suggestionAdapter);

        // 3. Search Logic
        setupSearchLogic();

        // 4. Featured Click
        cardFeaturedCollection.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BookListActivity.class);
            intent.putExtra("title", "Bộ sưu tập nổi bật");
            intent.putExtra("filter", "rating_avg=gte.4.5"); //lọc các sách đánh giá cao
            startActivity(intent);
        });

        loadFeaturedImages(view.findViewById(R.id.vfFeaturedFlipper), view.findViewById(R.id.ivFeaturedBg));
        
        setupMiniPlayer(view);
    }

    private void loadFeaturedImages(android.widget.ViewFlipper flipper, ImageView ivBg) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?rating_avg=gte.4.5&select=cover_url&limit=5";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
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
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                flipper.removeAllViews();
                                for (int i = 0; i < array.length(); i++) {
                                    String coverUrl = array.optJSONObject(i).optString("cover_url");
                                    if (!coverUrl.isEmpty()) {
                                        if (i == 0) {
                                            Glide.with(DiscoveryFragment.this).load(coverUrl).into(ivBg);
                                            updateFeaturedPalette(coverUrl);
                                        }
                                        ImageView iv = new ImageView(getContext());
                                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        Glide.with(DiscoveryFragment.this)
                                                .load(coverUrl)
                                                .placeholder(R.drawable.bacl)
                                                .into(iv);
                                        flipper.addView(iv);
                                    }
                                }
                                if (flipper.getChildCount() > 1) {
                                    flipper.startFlipping();
                                }
                            });
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void updateFeaturedPalette(String url) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            if (palette != null && isAdded()) {
                                int color = palette.getDarkVibrantColor(palette.getDominantColor(android.graphics.Color.parseColor("#6750A4")));
                                int finalColor = ColorUtils.blendARGB(color, android.graphics.Color.BLACK, 0.2f);
                                cardFeaturedCollection.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(finalColor));
                            }
                        });
                    }
                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
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

                android.widget.ImageButton btnPlay = miniPlayer.findViewById(R.id.btnMiniPlayPause);
                if (btnPlay != null) {
                    btnPlay.setImageResource(PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                }
                
                handleRotation(ivCover, PlayerManager.getInstance().isPlaying());
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
                searchHandler.postDelayed(this, 1000);
            }
        };
        searchHandler.postDelayed(progressRunnable, 1000);
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) searchHandler.removeCallbacks(progressRunnable);
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

    private void setupSearchLogic() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    cardSuggestions.setVisibility(View.GONE);
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    searchRunnable = () -> performSearch(keyword);
                    searchHandler.postDelayed(searchRunnable, 300);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        ivSearchAction.setOnClickListener(v -> performSearch(etSearch.getText().toString().trim()));

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSearch.getText().length() > 0) {
                cardSuggestions.setVisibility(View.VISIBLE);
            } else {
                searchHandler.postDelayed(() -> cardSuggestions.setVisibility(View.GONE), 200);
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            suggestionList.clear();
            suggestionAdapter.notifyDataSetChanged();
            cardSuggestions.setVisibility(View.GONE);
        });
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) return;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/search_books";
        JSONObject json = new JSONObject();
        try {
            json.put("query_text", keyword);
            json.put("category_filter", null);
            json.put("author_filter", null);
            json.put("limit_val", 10);
            json.put("offset_val", 0);
        } catch (Exception e) { return; }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Discovery", "Search failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        suggestionList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Book b = new Book();
                            b.setId(obj.getString("id"));
                            b.setTitle(obj.getString("title"));
                            b.setCoverUrl(obj.optString("cover_url"));
                            b.setAuthorName(obj.optString("author_name"));
                            b.setPremiumOnly(obj.optBoolean("is_premium_only"));
                            suggestionList.add(b);
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                suggestionAdapter.notifyDataSetChanged();
                                cardSuggestions.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Discovery", "Search parse error", e);
                    }
                }
            }
        });
    }

    private void loadCategories() {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/categories?select=id,name_vi,name_en,icon_url,gradient_colors";
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
                        List<Category> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Category c = new Category();
                            c.setId(obj.getString("id"));
                            c.setNameVi(obj.getString("name_vi"));
                            c.setIconUrl(obj.optString("icon_url"));
                            list.add(c);
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                DiscoveryCategoryAdapter adapter = new DiscoveryCategoryAdapter(list);
                                rvDiscoveryCategories.setAdapter(adapter);
                            });
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }
}
