package com.example.a23110035_23110060.view.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;
import com.example.a23110035_23110060.controller.PlayerManager;
import com.example.a23110035_23110060.controller.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AudioBookmarkListFragment extends BottomSheetDialogFragment {

    private String bookId;
    private RecyclerView rvBookmarks;
    private BookmarkAdapter adapter;
    private List<JSONObject> bookmarksList = new ArrayList<>();

    public AudioBookmarkListFragment() {}

    public static AudioBookmarkListFragment newInstance(String bookId) {
        AudioBookmarkListFragment fragment = new AudioBookmarkListFragment();
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
        View view = inflater.inflate(R.layout.layout_bottom_sheet_bookmark_list, container, false);
        
        rvBookmarks = view.findViewById(R.id.rv_bookmarks);
        rvBookmarks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookmarkAdapter();
        rvBookmarks.setAdapter(adapter);

        fetchBookmarks(view);
        
        return view;
    }

    private void fetchBookmarks(View view) {
        SessionManager sessionManager = new SessionManager(requireContext());
        String userId = sessionManager.getUserId();
        
        if (userId == null) return;

        String token = sessionManager.getAccessToken();
        String authHeader = (token != null) ? "Bearer " + token : "Bearer " + BuildConfig.SUPABASE_ANON_KEY;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&book_id=eq." + bookId + "&position_seconds=not.is.null&order=position_seconds.asc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeader)
                .get()
                .build();

        com.example.a23110035_23110060.controller.NetworkClient.getClient(requireContext()).newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray arr = new JSONArray(response.body().string());
                        bookmarksList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            bookmarksList.add(arr.getJSONObject(i));
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                TextView tvCount = view.findViewById(R.id.tv_bookmark_count);
                                if (tvCount != null) {
                                    tvCount.setText("Đánh dấu (" + bookmarksList.size() + ")");
                                }
                            });
                        }
                    } catch (Exception e) {}
                }
            }
        });
    }

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject obj = bookmarksList.get(position);
            try {
                int posSeconds = obj.getInt("position_seconds");
                String note = obj.optString("note", "");
                
                int minutes = posSeconds / 60;
                int seconds = posSeconds % 60;
                
                PlayerManager pm = PlayerManager.getInstance();
                String chapterTitle = "ĐÁNH DẤU";
                if (pm.getCurrentChapterTitle() != null && !pm.getCurrentChapterTitle().isEmpty()) {
                    chapterTitle = pm.getCurrentChapterTitle().toUpperCase();
                }
                
                holder.tvChapterTitle.setText(chapterTitle);
                holder.tvTime.setText("Trích đoạn " + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                holder.tvNote.setText(note);
                
                holder.itemView.setOnClickListener(v -> {
                    PlayerManager.getInstance().seekTo(posSeconds * 1000);
                    dismiss();
                });
            } catch (Exception e) {}
        }

        @Override
        public int getItemCount() {
            return bookmarksList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvChapterTitle, tvTime, tvNote;
            ViewHolder(View itemView) {
                super(itemView);
                tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvNote = itemView.findViewById(R.id.tv_note);
            }
        }
    }
}
