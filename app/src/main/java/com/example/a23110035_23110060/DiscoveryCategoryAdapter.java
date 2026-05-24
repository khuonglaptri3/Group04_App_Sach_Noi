package com.example.a23110035_23110060;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
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
            holder.rlBackground.setBackgroundColor(0xFF6750A4);
        }
        
        // Xử lý count (số lượng sách)
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
            Toast.makeText(v.getContext(), "Danh mục: " + category.getNameVi(), Toast.LENGTH_SHORT).show();
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