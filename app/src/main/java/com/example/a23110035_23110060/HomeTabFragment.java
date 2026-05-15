package com.example.a23110035_23110060;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeTabFragment extends Fragment {

    public static HomeTabFragment newInstance(String type) {
        HomeTabFragment fragment = new HomeTabFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView rvSections = view.findViewById(R.id.rvSections);
        rvSections.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<Section> sections = new ArrayList<>();
        
        // Mock books
        List<Book> trendingBooks = new ArrayList<>();
        trendingBooks.add(new Book("Đắc Nhân Tâm", "Dale Carnegie", R.drawable.bacl, true));
        trendingBooks.add(new Book("Sapiens", "Y. N. Harari", R.drawable.bacl, false));
        trendingBooks.add(new Book("Nhà Giả Kim", "Paulo Coelho", R.drawable.bacl, true));
        
        // 1. Trending
        sections.add(new Section("THỊNH HÀNH HÔM NAY", trendingBooks, Section.TYPE_CAROUSEL));
        
        // 2. Banner
        sections.add(new Section("BANNER", (List<Book>)null, Section.TYPE_BANNER));
        
        // 3. Categories
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Thiền định", R.drawable.ic_brain, Color.parseColor("#8E2DE2")));
        categories.add(new Category("Kinh doanh", R.drawable.ic_search, Color.parseColor("#1A237E")));
        categories.add(new Category("Thiếu nhi", R.drawable.ic_heart, Color.parseColor("#FF6B9D")));
        categories.add(new Category("Lịch sử", R.drawable.ic_back, Color.parseColor("#F7971E")));
        categories.add(new Category("Tiếng Anh", R.drawable.ic_person, Color.parseColor("#56AB2F")));
        
        sections.add(new Section("DANH MỤC", categories, Section.TYPE_CATEGORIES, true));
        
        // 4. New Releases
        sections.add(new Section("MỚI XUẤT BẢN", trendingBooks, Section.TYPE_CAROUSEL));
        
        rvSections.setAdapter(new SectionAdapter(sections));
    }
}