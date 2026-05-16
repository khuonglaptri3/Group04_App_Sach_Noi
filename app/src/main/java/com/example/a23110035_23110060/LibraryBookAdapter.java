package com.example.a23110035_23110060;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;

public class LibraryBookAdapter extends RecyclerView.Adapter<LibraryBookAdapter.LibraryViewHolder> {

    private List<LibraryBook> bookList;

    public LibraryBookAdapter(List<LibraryBook> bookList) {
        this.bookList = bookList;
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
        holder.ivCover.setImageResource(book.getCoverResId());
        
        // Premium Badge
        holder.tvPremium.setVisibility(book.isPremium() ? View.VISIBLE : View.GONE);
        
        // Progress Info
        holder.tvProgressPercent.setText(book.getProgress() + "% hoàn thành");
        holder.tvTimeRemaining.setText(book.getTimeRemaining());
        holder.pbProgress.setProgress(book.getProgress());
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvAuthor, tvPremium, tvProgressPercent, tvTimeRemaining;
        LinearProgressIndicator pbProgress;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivBookCover);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthorName);
            tvPremium = itemView.findViewById(R.id.tvPremiumBadge);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            pbProgress = itemView.findViewById(R.id.pbBookProgress);
        }
    }
}