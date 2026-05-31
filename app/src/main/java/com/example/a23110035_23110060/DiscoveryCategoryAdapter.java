package com.example.a23110035_23110060;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DiscoveryCategoryAdapter extends RecyclerView.Adapter<DiscoveryCategoryAdapter.DiscoveryViewHolder> {

    private List<Category> categories;
    private OkHttpClient client;

    public DiscoveryCategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public DiscoveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        client = NetworkClient.getClient(parent.getContext());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discovery_category, parent, false);
        return new DiscoveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscoveryViewHolder holder, int position) {
        Category category = categories.get(position);
        
        holder.tvName.setText(category.getNameVi());
        
        if (category.getCount() != null && !category.getCount().isEmpty()) {
            holder.tvCount.setVisibility(View.VISIBLE);
            holder.tvCount.setText(category.getCount());
        } else {
            holder.tvCount.setVisibility(View.GONE);
        }

        if (category.getBadge() != null && !category.getBadge().isEmpty()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(category.getBadge());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        loadCategoryData(category.getId(), holder);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BookListActivity.class);
            intent.putExtra("title", category.getNameVi());
            intent.putExtra("filter", "category_id=eq." + category.getId());
            v.getContext().startActivity(intent);
        });
    }

    private void loadCategoryData(String categoryId, DiscoveryViewHolder holder) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/books?category_id=eq." + categoryId + "&select=cover_url&limit=5";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        holder.itemView.post(() -> {
                            holder.vfImages.removeAllViews();
                            for (int i = 0; i < array.length(); i++) {
                                String coverUrl = array.optJSONObject(i).optString("cover_url");
                                if (!coverUrl.isEmpty()) {
                                    // Extract color from the very first book cover
                                    if (i == 0) {
                                        updatePaletteFromUrl(coverUrl, holder);
                                        // Also show first cover in background stack
                                        Glide.with(holder.itemView.getContext())
                                                .load(coverUrl)
                                                .placeholder(R.drawable.bacl)
                                                .into(holder.ivCoverBg);
                                    }
                                    
                                    ImageView iv = new ImageView(holder.vfImages.getContext());
                                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    Glide.with(holder.vfImages.getContext())
                                            .load(coverUrl)
                                            .placeholder(R.drawable.bacl)
                                            .into(iv);
                                    holder.vfImages.addView(iv);
                                }
                            }
                            if (holder.vfImages.getChildCount() > 1) {
                                holder.vfImages.startFlipping();
                            }
                        });
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void updatePaletteFromUrl(String url, DiscoveryViewHolder holder) {
        Glide.with(holder.itemView.getContext())
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                // Prefer dark vibrant or dominant
                                int color = palette.getDarkVibrantColor(
                                        palette.getDominantColor(Color.parseColor("#6750A4")));
                                
                                // Make it a bit darker and saturated for text readability
                                int finalColor = ColorUtils.blendARGB(color, Color.BLACK, 0.2f);
                                holder.cardCategory.setCardBackgroundColor(ColorStateList.valueOf(finalColor));
                            }
                        });
                    }
                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class DiscoveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount, tvBadge;
        ViewFlipper vfImages;
        ImageView ivCoverBg;
        MaterialCardView cardCategory;

        public DiscoveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvCount = itemView.findViewById(R.id.tvCategoryCount);
            tvBadge = itemView.findViewById(R.id.tvCategoryBadge);
            vfImages = itemView.findViewById(R.id.vfCategoryImages);
            ivCoverBg = itemView.findViewById(R.id.ivCoverBg);
            cardCategory = itemView.findViewById(R.id.cardCategory);
        }
    }
}
