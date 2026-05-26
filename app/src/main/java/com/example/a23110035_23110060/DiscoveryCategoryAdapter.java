package com.example.a23110035_23110060;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DiscoveryCategoryAdapter extends RecyclerView.Adapter<DiscoveryCategoryAdapter.DiscoveryViewHolder> {

    private List<Category> categories;

    public DiscoveryCategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public DiscoveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discovery_category, parent, false);
        return new DiscoveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscoveryViewHolder holder, int position) {
        Category category = categories.get(position);
        
        holder.tvName.setText(category.getNameVi());
        
        if (category.getBackgroundRes() != 0) {
            holder.rlBackground.setBackgroundResource(category.getBackgroundRes());
        } else {
            // Assign a default colorful background based on position if no background is set
            int[] colors = {0xFF6750A4, 0xFF4F378A, 0xFFfe6a34, 0xFF56AB2F, 0xFF8E2DE2, 0xFFF7971E};
            holder.rlBackground.setBackgroundColor(colors[position % colors.length]);
        }
        
        // Xử lý count (số lượng sách) - can be actual data from DB if available
        if (category.getCount() != null && !category.getCount().isEmpty()) {
            holder.tvCount.setVisibility(View.VISIBLE);
            holder.tvCount.setText(category.getCount());
        } else {
            holder.tvCount.setVisibility(View.GONE);
        }

        // Xử lý badge (Hot, Mới nhất...)
        if (category.getBadge() != null && !category.getBadge().isEmpty()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(category.getBadge());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BookListActivity.class);
            intent.putExtra("title", category.getNameVi());
            intent.putExtra("filter", "category_id=eq." + category.getId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class DiscoveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount, tvBadge;
        RelativeLayout rlBackground;

        public DiscoveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvCount = itemView.findViewById(R.id.tvCategoryCount);
            tvBadge = itemView.findViewById(R.id.tvCategoryBadge);
            rlBackground = itemView.findViewById(R.id.rlBackground);
        }
    }
}