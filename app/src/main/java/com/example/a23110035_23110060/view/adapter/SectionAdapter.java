package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.BookListActivity;

import com.example.a23110035_23110060.model.Section;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Section> sections;

    public SectionAdapter(List<Section> sections) {
        this.sections = sections;
    }

    @Override
    public int getItemViewType(int position) {
        return sections.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Section.TYPE_BANNER) {
            return new BannerViewHolder(inflater.inflate(R.layout.item_section_banner, parent, false));
        } else if (viewType == Section.TYPE_CATEGORIES) {
            return new CategoriesViewHolder(inflater.inflate(R.layout.item_section_categories, parent, false));
        } else if (viewType == Section.TYPE_FEATURED_REVIEWS) {
            return new FeaturedReviewsViewHolder(inflater.inflate(R.layout.item_section_carousel, parent, false));
        } else {
            return new CarouselViewHolder(inflater.inflate(R.layout.item_section_carousel, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Section section = sections.get(position);
        if (holder instanceof CarouselViewHolder) {
            CarouselViewHolder h = (CarouselViewHolder) holder;
            h.tvTitle.setText(section.getTitle());
            BookAdapter adapter = new BookAdapter(section.getBooks());
            if (section.getFilter() != null) {
                if (section.getFilter().contains("is_ebook=eq.true")) {
                    adapter.setPreferredType("ebook");
                } else if (section.getFilter().contains("is_audiobook=eq.true")) {
                    adapter.setPreferredType("audio");
                }
            }
            h.rvCarousel.setAdapter(adapter);
            h.rvCarousel.setLayoutManager(new LinearLayoutManager(h.itemView.getContext(), RecyclerView.HORIZONTAL, false));
            
            h.tvSeeAll.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), BookListActivity.class);
                intent.putExtra("title", section.getTitle());
                intent.putExtra("filter", section.getFilter());
                v.getContext().startActivity(intent);
            });
        } else if (holder instanceof BannerViewHolder) {
            BannerViewHolder h = (BannerViewHolder) holder;
            h.itemView.findViewById(R.id.cardBanner).setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Nhận quà", Toast.LENGTH_SHORT).show();
            });
        } else if (holder instanceof CategoriesViewHolder) {
            CategoriesViewHolder h = (CategoriesViewHolder) holder;
            h.rvCategories.setAdapter(new CategoryAdapter(section.getCategories()));
            h.rvCategories.setLayoutManager(new LinearLayoutManager(h.itemView.getContext(), RecyclerView.HORIZONTAL, false));
        } else if (holder instanceof FeaturedReviewsViewHolder) {
            FeaturedReviewsViewHolder h = (FeaturedReviewsViewHolder) holder;
            h.tvTitle.setText(section.getTitle());
            h.rvCarousel.setAdapter(new FeaturedReviewAdapter(section.getFeaturedReviews()));
            h.rvCarousel.setLayoutManager(new LinearLayoutManager(h.itemView.getContext(), RecyclerView.HORIZONTAL, false));
            h.tvSeeAll.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSeeAll;
        RecyclerView rvCarousel;
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSeeAll = itemView.findViewById(R.id.tvSeeAll);
            rvCarousel = itemView.findViewById(R.id.rvCarousel);
        }
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        public BannerViewHolder(@NonNull View itemView) { super(itemView); }
    }

    static class CategoriesViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvCategories;
        public CategoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            rvCategories = itemView.findViewById(R.id.rvCategories);
        }
    }

    static class FeaturedReviewsViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSeeAll;
        RecyclerView rvCarousel;
        public FeaturedReviewsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSeeAll = itemView.findViewById(R.id.tvSeeAll);
            rvCarousel = itemView.findViewById(R.id.rvCarousel);
        }
    }
}