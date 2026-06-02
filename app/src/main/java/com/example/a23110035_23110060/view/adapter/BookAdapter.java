package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;



import com.example.a23110035_23110060.model.Book;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private final List<Book> books;
    private int layoutResId = R.layout.item_book_card; // Mặc định
    private String preferredType = null;

    public BookAdapter(List<Book> books) {
        this.books = books;
    }

    public void setPreferredType(String preferredType) {
        this.preferredType = preferredType;
    }

    public BookAdapter(List<Book> books, int layoutResId) {
        this.books = books;
        this.layoutResId = layoutResId;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.tvTitle.setText(book.getTitle());
        holder.tvAuthor.setText(book.getAuthorName());
        
        Glide.with(holder.itemView.getContext())
            .load(book.getCoverUrl())
            .placeholder(R.drawable.bacl)
            .error(R.drawable.bacl)
            .into(holder.ivCover);

        holder.badgePremium.setVisibility(book.isPremiumOnly() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            Class<?> activityClass;
            if ("ebook".equals(preferredType)) {
                activityClass = com.example.a23110035_23110060.view.activity.EbookDetailActivity.class;
            } else if ("audio".equals(preferredType)) {
                activityClass = com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class;
            } else {
                // Mặc định ưu tiên audiobook nếu cả 2, hoặc tùy theo cờ
                activityClass = book.isAudiobook() ? 
                    com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class : 
                    com.example.a23110035_23110060.view.activity.EbookDetailActivity.class;
            }
                
            Intent intent = new Intent(v.getContext(), activityClass);
            intent.putExtra("bookId", book.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor;
        View badgePremium;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivBookCover);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor = itemView.findViewById(R.id.tvBookAuthor);
            badgePremium = itemView.findViewById(R.id.cardBadgePremium);
        }
    }
}