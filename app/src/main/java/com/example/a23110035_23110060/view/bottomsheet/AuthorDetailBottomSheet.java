package com.example.a23110035_23110060.view.bottomsheet;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.BookDetailActivity;

import com.example.a23110035_23110060.model.Book;
import com.example.a23110035_23110060.controller.SessionManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

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

public class AuthorDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_AUTHOR_ID = "author_id";
    private static final String ARG_AUTHOR_NAME = "author_name";
    private static final String ARG_AUTHOR_BIO = "author_bio";
    private static final String ARG_AUTHOR_AVATAR = "author_avatar";

    private String authorId;
    private String authorName;
    private String authorBio;
    private String authorAvatar;

    private boolean isBioExpanded = false;
    private List<Book> allBooks = new ArrayList<>();
    private List<Book> filteredBooks = new ArrayList<>();
    private AuthorBookAdapter adapter;

    private ProgressBar pbLoading;
    private RecyclerView rvBooks;
    private TextView tvEmpty;
    private Chip chipAll, chipAudiobooks, chipEbooks;

    private final OkHttpClient client = new OkHttpClient();

    public static AuthorDetailBottomSheet newInstance(String authorId, String authorName, String authorBio, String authorAvatar) {
        AuthorDetailBottomSheet fragment = new AuthorDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_AUTHOR_ID, authorId);
        args.putString(ARG_AUTHOR_NAME, authorName);
        args.putString(ARG_AUTHOR_BIO, authorBio);
        args.putString(ARG_AUTHOR_AVATAR, authorAvatar);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            authorId = getArguments().getString(ARG_AUTHOR_ID);
            authorName = getArguments().getString(ARG_AUTHOR_NAME);
            authorBio = getArguments().getString(ARG_AUTHOR_BIO);
            authorAvatar = getArguments().getString(ARG_AUTHOR_AVATAR);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_author_detail_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        ImageView ivAvatar = view.findViewById(R.id.iv_author_avatar);
        TextView tvNameHeader = view.findViewById(R.id.tv_author_name_header);
        TextView tvBio = view.findViewById(R.id.tv_author_bio);
        TextView btnBioReadMore = view.findViewById(R.id.btn_bio_read_more);
        View btnClose = view.findViewById(R.id.btn_close);

        pbLoading = view.findViewById(R.id.pb_loading);
        rvBooks = view.findViewById(R.id.rv_author_books);
        tvEmpty = view.findViewById(R.id.tv_empty);
        chipAll = view.findViewById(R.id.chip_all);
        chipAudiobooks = view.findViewById(R.id.chip_audiobooks);
        chipEbooks = view.findViewById(R.id.chip_ebooks);

        // Header Info
        tvNameHeader.setText(authorName);
        if (authorAvatar != null && !authorAvatar.isEmpty()) {
            Glide.with(this)
                    .load(authorAvatar)
                    .placeholder(R.drawable.bacl)
                    .error(R.drawable.bacl)
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.bacl);
        }

        // Bio section
        if (authorBio == null || authorBio.trim().isEmpty() || authorBio.equalsIgnoreCase("null")) {
            tvBio.setText("Chưa có thông tin giới thiệu về tác giả.");
            btnBioReadMore.setVisibility(View.GONE);
        } else {
            tvBio.setText(authorBio);
            tvBio.setMaxLines(3);
            
            // Check if text actually exceeds 3 lines to show Read More
            tvBio.setMaxLines(4); // Temporarily allow 4 lines to check if it's long
            tvBio.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (tvBio.getLayout() != null) {
                        tvBio.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int lineCount = tvBio.getLineCount();
                        if (lineCount > 3) {
                            btnBioReadMore.setVisibility(View.VISIBLE);
                        } else {
                            btnBioReadMore.setVisibility(View.GONE);
                        }
                        // Re-apply truncation if not expanded
                        if (!isBioExpanded) {
                            tvBio.setMaxLines(3);
                        }
                    }
                }
            });

            btnBioReadMore.setOnClickListener(v -> {
                isBioExpanded = !isBioExpanded;
                if (isBioExpanded) {
                    tvBio.setMaxLines(Integer.MAX_VALUE);
                    btnBioReadMore.setText("Thu gọn");
                } else {
                    tvBio.setMaxLines(3);
                    btnBioReadMore.setText("Xem thêm");
                }
            });
        }

        btnClose.setOnClickListener(v -> dismiss());

        // Recycler view
        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AuthorBookAdapter(filteredBooks);
        rvBooks.setAdapter(adapter);

        // Setup filter listeners
        chipAll.setOnClickListener(v -> applyFilter("all"));
        chipAudiobooks.setOnClickListener(v -> applyFilter("audio"));
        chipEbooks.setOnClickListener(v -> applyFilter("ebook"));

        fetchAuthorBooks();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Fully expand the bottom sheet dialog by default
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void fetchAuthorBooks() {
        if (authorId == null || authorId.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            pbLoading.setVisibility(View.GONE);
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Context context = getContext();
        if (context == null) return;

        SessionManager sessionManager = new SessionManager(context);
        String token = sessionManager.getAccessToken();

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?author_id=eq." + authorId + "&select=*,authors(name)";

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);

        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        } else {
            builder.addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("AuthorDetail", "Failed to fetch books", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Không thể tải danh sách tác phẩm", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        allBooks.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Book book = new Book();
                            book.setId(obj.getString("id"));
                            book.setTitle(obj.getString("title"));
                            book.setCoverUrl(obj.optString("cover_url"));
                            book.setPremiumOnly(obj.optBoolean("is_premium_only", false));
                            book.setAudiobook(obj.optBoolean("is_audiobook", true));
                            book.setEbook(obj.optBoolean("is_ebook", false));

                            JSONObject authorObj = obj.optJSONObject("authors");
                            book.setAuthorName(authorObj != null ? authorObj.optString("name", "Unknown") : "Unknown");

                            allBooks.add(book);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                pbLoading.setVisibility(View.GONE);
                                updateTabCounts();
                                applyFilter("all");
                            });
                        }
                    } catch (Exception e) {
                        Log.e("AuthorDetail", "Error parsing books JSON", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> pbLoading.setVisibility(View.GONE));
                        }
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> pbLoading.setVisibility(View.GONE));
                    }
                }
            }
        });
    }

    private void updateTabCounts() {
        int audiobooksCount = 0;
        int ebooksCount = 0;

        for (Book book : allBooks) {
            if (book.isAudiobook()) {
                audiobooksCount++;
            }
            if (book.isEbook()) {
                ebooksCount++;
            }
        }

        chipAll.setText("Tất cả (" + allBooks.size() + ")");
        chipAudiobooks.setText("Sách nói (" + audiobooksCount + ")");
        chipEbooks.setText("Sách điện tử (" + ebooksCount + ")");
    }

    private void applyFilter(String type) {
        filteredBooks.clear();
        chipAll.setChecked(type.equals("all"));
        chipAudiobooks.setChecked(type.equals("audio"));
        chipEbooks.setChecked(type.equals("ebook"));

        if (type.equals("all")) {
            filteredBooks.addAll(allBooks);
        } else if (type.equals("audio")) {
            for (Book book : allBooks) {
                if (book.isAudiobook()) {
                    filteredBooks.add(book);
                }
            }
        } else if (type.equals("ebook")) {
            for (Book book : allBooks) {
                if (book.isEbook()) {
                    filteredBooks.add(book);
                }
            }
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredBooks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Custom RecyclerView Adapter inside the fragment
    private class AuthorBookAdapter extends RecyclerView.Adapter<AuthorBookAdapter.ViewHolder> {

        private final List<Book> books;

        public AuthorBookAdapter(List<Book> books) {
            this.books = books;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_author_book, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Book book = books.get(position);
            holder.tvTitle.setText(book.getTitle());
            holder.tvAuthor.setText(book.getAuthorName());

            // Image
            Glide.with(holder.itemView.getContext())
                    .load(book.getCoverUrl())
                    .placeholder(R.drawable.bacl)
                    .error(R.drawable.bacl)
                    .into(holder.ivCover);

            // Badges
            if (book.isAudiobook()) {
                holder.tvTypeBadge.setVisibility(View.VISIBLE);
                holder.tvTypeBadge.setText("SÁCH NÓI");
                holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_pill_purple_light);
                holder.tvTypeBadge.setTextColor(0xFF4F378A);
            } else if (book.isEbook()) {
                holder.tvTypeBadge.setVisibility(View.VISIBLE);
                holder.tvTypeBadge.setText("SÁCH ĐIỆN TỬ");
                holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_pill);
                holder.tvTypeBadge.setTextColor(0xFF00796B); // Clean dark green teal color
            } else {
                holder.tvTypeBadge.setVisibility(View.GONE);
            }

            holder.tvPremiumBadge.setVisibility(book.isPremiumOnly() ? View.VISIBLE : View.GONE);

            // Navigation to detail
            View.OnClickListener clickListener = v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, BookDetailActivity.class);
                intent.putExtra("bookId", book.getId());
                context.startActivity(intent);
                dismiss(); // Dismiss bottom sheet when opening book
            };

            holder.itemView.setOnClickListener(clickListener);
            holder.btnPlay.setOnClickListener(clickListener);
            holder.btnMore.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Tùy chọn: " + book.getTitle(), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle, tvAuthor, tvTypeBadge, tvPremiumBadge;
            View btnPlay;
            View btnMore;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.ivBookCover);
                tvTitle = itemView.findViewById(R.id.tvBookTitle);
                tvAuthor = itemView.findViewById(R.id.tvBookAuthor);
                tvTypeBadge = itemView.findViewById(R.id.tvBookTypeBadge);
                tvPremiumBadge = itemView.findViewById(R.id.tvPremiumBadge);
                btnPlay = itemView.findViewById(R.id.btnPlayBook);
                btnMore = itemView.findViewById(R.id.btnMoreOptions);
            }
        }
    }
}
