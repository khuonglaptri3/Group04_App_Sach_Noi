package com.example.a23110035_23110060;

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

    public EbookPagerAdapter(List<CharSequence> pages) {
        this.pages = pages;
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
        holder.tvPageContent.setText(pages.get(position));
        holder.tvPageContent.setTextSize(currentTextSize);
        holder.tvPageContent.setTextColor(currentTextColor);

        // Đảm bảo TextView luôn có thể chọn được sau khi bị recycle
        holder.tvPageContent.setTextIsSelectable(true);
        holder.tvPageContent.setFocusable(true);
        holder.tvPageContent.setFocusableInTouchMode(true);

        if (customSelectionCallback != null) {
            holder.tvPageContent.setCustomSelectionActionModeCallback(customSelectionCallback);
        }
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
