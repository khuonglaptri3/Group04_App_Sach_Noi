package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EbookPagerAdapter extends RecyclerView.Adapter<EbookPagerAdapter.PageViewHolder> {

    private List<CharSequence> pages;
    private float currentTextSize = 18f;
    private int currentTextColor = 0xFF1D1B20;
    private android.view.ActionMode.Callback customSelectionCallback;
    private String searchQuery = "";

    public EbookPagerAdapter(List<CharSequence> pages) {
        this.pages = pages;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        notifyDataSetChanged();
    }

    public void updatePages(List<CharSequence> newPages) {
        this.pages = newPages;
        notifyDataSetChanged();
    }

    public void setTextSize(float textSize) {
        this.currentTextSize = textSize;
        notifyDataSetChanged();
    }

    public void setTextColor(int color) {
        this.currentTextColor = color;
        notifyDataSetChanged();
    }

    public void setCustomSelectionCallback(android.view.ActionMode.Callback callback) {
        this.customSelectionCallback = callback;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ebook_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        CharSequence pageContent = pages.get(position);
        
        if (searchQuery != null && !searchQuery.isEmpty()) {
            android.text.SpannableString spannable = new android.text.SpannableString(pageContent);
            String textLower = pageContent.toString().toLowerCase();
            String queryLower = searchQuery.toLowerCase();
            int index = textLower.indexOf(queryLower);
            while (index >= 0) {
                spannable.setSpan(new android.text.style.BackgroundColorSpan(0x80FFFF00), index, index + queryLower.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = textLower.indexOf(queryLower, index + queryLower.length());
            }
            holder.tvPageContent.setText(spannable);
        } else {
            holder.tvPageContent.setText(pageContent);
        }
        
        holder.tvPageContent.setTextSize(currentTextSize);
        holder.tvPageContent.setTextColor(currentTextColor);

        // Đảm bảo TextView luôn có thể chọn được sau khi bị recycle
        holder.tvPageContent.setTextIsSelectable(true);
        holder.tvPageContent.setFocusable(true);
        holder.tvPageContent.setFocusableInTouchMode(true);

        if (customSelectionCallback != null) {
            holder.tvPageContent.setCustomSelectionActionModeCallback(customSelectionCallback);
        }
        
        // Ngăn View bị tái sử dụng để tránh lỗi kẹt SelectionActionMode của TextView
        holder.setIsRecyclable(false);
    }

    @Override
    public int getItemCount() {
        return pages == null ? 0 : pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        TextView tvPageContent;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPageContent = itemView.findViewById(R.id.tv_page_content);
        }
    }
}
