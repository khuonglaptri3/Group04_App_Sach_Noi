package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;



import com.example.a23110035_23110060.model.Book;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
    private final List<Book> suggestions;

    public SuggestionAdapter(List<Book> suggestions) {
        this.suggestions = suggestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = suggestions.get(position);
        holder.tvTitle.setText(book.getTitle());
        holder.itemView.setOnClickListener(v -> {
            // Using bookType to decide Activity
            Class<?> activityClass = book.isEbook() ? 
                com.example.a23110035_23110060.view.activity.EbookDetailActivity.class : 
                com.example.a23110035_23110060.view.activity.AudiobookDetailActivity.class;
            Intent intent = new Intent(v.getContext(), activityClass);
            intent.putExtra("bookId", book.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSuggestionTitle);
        }
    }
}