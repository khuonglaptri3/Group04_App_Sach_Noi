package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.view.fragment.HomeTabFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HomePagerAdapter extends FragmentStateAdapter {

    private String[] titles;

    public HomePagerAdapter(@NonNull Fragment fragment, String[] titles) {
        super(fragment);
        this.titles = titles;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String type = (position == 0) ? "audio" : "ebook";
        return HomeTabFragment.newInstance(type);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}