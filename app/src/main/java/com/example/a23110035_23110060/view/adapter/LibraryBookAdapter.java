package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;


import com.example.a23110035_23110060.model.LibraryBook;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;

public class LibraryBookAdapter extends RecyclerView.Adapter<LibraryBookAdapter.LibraryViewHolder> {

    private final List<LibraryBook> bookList;
    private final OnItemDeleteListener deleteListener;
    private String preferredType = null;

    public interface OnItemDeleteListener {
        void onDelete(LibraryBook book, int position);
    }

    public LibraryBookAdapter(List<LibraryBook> bookList, OnItemDeleteListener deleteListener) {
        this.bookList = bookList;
        this.deleteListener = deleteListener;
    }

    public void setPreferredType(String preferredType) {
        this.preferredType = preferredType;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_library_book, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryBook book = bookList.get(position);

        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthor());
        
        if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(R.drawable.bacl)
                .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(book.getCoverResId());
        }
        
        // Premium Badge
        holder.tvPremium.setVisibility(book.isPremium() ? View.VISIBLE : View.GONE);
        
        // Progress Info
        holder.tvProgressPercent.setText(book.getProgress() + "% hoàn thành");
        holder.tvTimeRemaining.setText(book.getTimeRemaining());
        holder.pbProgress.setProgress(book.getProgress());

        holder.itemView.setOnClickListener(v -> {
            Class<?> activityClass;
            if ("ebook".equals(preferredType)) {
                activityClass = com.example.a23110035_23110060.view.activity.EbookDetailActivity.class;
            } else if ("audiobook".equals(preferredType)) {
                activityClass = com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class;
            } else {
                activityClass = book.isAudiobook() ? 
                    com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class : 
                    com.example.a23110035_23110060.view.activity.EbookDetailActivity.class;
            }
            Intent intent = new Intent(v.getContext(), activityClass);
            intent.putExtra("bookId", book.getBookId());
            v.getContext().startActivity(intent);
        });

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add("Xóa khỏi thư viện");
            popup.getMenu().add("Tải xuống");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Xóa khỏi thư viện")) {
                    if (deleteListener != null) {
                        deleteListener.onDelete(book, position);
                    }
                } else {
                    Toast.makeText(v.getContext(), item.getTitle() + ": " + book.getTitle(), Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvPremium, tvProgressPercent, tvTimeRemaining;
        LinearProgressIndicator pbProgress;
        View btnMore;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivBookCover);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthorName);
            tvPremium = itemView.findViewById(R.id.tvPremiumBadge);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            pbProgress = itemView.findViewById(R.id.pbBookProgress);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}