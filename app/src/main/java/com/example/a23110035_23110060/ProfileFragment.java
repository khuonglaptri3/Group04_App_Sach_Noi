package com.example.a23110035_23110060;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Sử dụng chung layout với HomeFragment (để có MiniPlayer và cấu trúc AppBar)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Tùy chỉnh phần AppBar: Ẩn lời chào và TabLayout
        View greetingLayout = view.findViewById(R.id.tvGreetingSub).getParent() instanceof View 
                ? (View) view.findViewById(R.id.tvGreetingSub).getParent() : null;
        if (greetingLayout != null) {
            greetingLayout.setVisibility(View.GONE);
        }

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            tabLayout.setVisibility(View.GONE);
        }

        // 2. Thiết lập ViewPager2 để hiển thị nội dung Hồ sơ
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new ProfileContentFragment();
            }

            @Override
            public int getItemCount() {
                return 1; // Chỉ có 1 trang Hồ sơ
            }
        });

        // 3. Hiển thị Mini Player (giống Home và Library)
        View miniPlayer = view.findViewById(R.id.miniPlayer);
        if (miniPlayer != null) {
            miniPlayer.setVisibility(View.VISIBLE);
        }
    }
}
