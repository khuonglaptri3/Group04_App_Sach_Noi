package com.example.a23110035_23110060;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());
        
        if (category.getIconUrl() != null && !category.getIconUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(category.getIconUrl())
                .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageResource(category.getIconResId());
        }

        // Use a default color if not specified
        int color = category.getColor() != 0 ? category.getColor() : Color.parseColor("#E0E0E0");
        holder.flIconWrap.setBackgroundTintList(ColorStateList.valueOf(color));

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

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        View flIconWrap;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            flIconWrap = itemView.findViewById(R.id.flIconWrap);
        }
    }
}