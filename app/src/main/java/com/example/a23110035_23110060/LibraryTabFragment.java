package com.example.a23110035_23110060;

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

public class LibraryTabFragment extends Fragment {

    public static LibraryTabFragment newInstance(String type) {
        LibraryTabFragment fragment = new LibraryTabFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Tái sử dụng layout của Home Tab (RecyclerView + SwipeRefresh)
        return inflater.inflate(R.layout.fragment_home_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView rvBooks = view.findViewById(R.id.rvSections);
        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        
        String type = getArguments() != null ? getArguments().getString("type") : "";
        
        // Chỉ hiển thị dữ liệu cho tab "Gần đây" để khớp với yêu cầu ảnh mẫu
        if ("Gần đây".equals(type)) {
            LibraryBookAdapter adapter = new LibraryBookAdapter(getMockLibraryBooks());
            rvBooks.setAdapter(adapter);
        } else {
            // Các tab khác hiện trống hoặc logic khác
            rvBooks.setAdapter(new LibraryBookAdapter(new ArrayList<>()));
        }
    }

    private List<LibraryBook> getMockLibraryBooks() {
        List<LibraryBook> list = new ArrayList<>();
        list.add(new LibraryBook("Sức Mạnh Của Sự Tĩnh Lặng", "Ryan Holiday", R.drawable.bacl, false, 75, "Còn 1g 15ph"));
        list.add(new LibraryBook("Nghệ Thuật Tư Duy Rành Mạch", "Rolf Dobelli", R.drawable.bacl, true, 12, "Còn 8g 45ph"));
        return list;
    }
}