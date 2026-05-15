package com.example.a23110035_23110060;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Section> sections;

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
            h.rvCarousel.setAdapter(new BookAdapter(section.getBooks()));
            h.rvCarousel.setLayoutManager(new LinearLayoutManager(h.itemView.getContext(), RecyclerView.HORIZONTAL, false));
        } else if (holder instanceof CategoriesViewHolder) {
            CategoriesViewHolder h = (CategoriesViewHolder) holder;
            h.rvCategories.setAdapter(new CategoryAdapter(section.getCategories()));
            h.rvCategories.setLayoutManager(new LinearLayoutManager(h.itemView.getContext(), RecyclerView.HORIZONTAL, false));
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RecyclerView rvCarousel;
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
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
}