package com.example.a23110035_23110060;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    private String[] titles = new String[]{"Gần đây", "Đã mua", "Đã tải", "Yêu thích"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Dùng chung layout với HomeFragment để giữ tính nhất quán (Toolbar, MiniPlayer, Tabs)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Tùy chỉnh phần tiêu đề để phù hợp với Thư viện
        TextView tvGreetingSub = view.findViewById(R.id.tvGreetingSub);
        TextView tvGreetingMain = view.findViewById(R.id.tvGreetingMain);
        
        if (tvGreetingSub != null) tvGreetingSub.setVisibility(View.GONE);
        if (tvGreetingMain != null) tvGreetingMain.setText("Thư viện");

        // 2. Thiết lập ViewPager và TabLayout (Dùng chung kiến trúc với Home)
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // Sử dụng một Fragment chuyên biệt cho nội dung danh sách sách trong thư viện
                return LibraryTabFragment.newInstance(titles[position]);
            }

            @Override
            public int getItemCount() {
                return titles.length;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(titles[position]);
        }).attach();

        // 3. Đảm bảo Mini Player được hiển thị (giống Home)
        View miniPlayer = view.findViewById(R.id.miniPlayer);
        if (miniPlayer != null) {
            miniPlayer.setVisibility(View.VISIBLE);
        }
    }
}
