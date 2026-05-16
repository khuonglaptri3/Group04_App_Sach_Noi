package com.example.a23110035_23110060;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryFragment extends Fragment {

    private RecyclerView rvDiscoveryCategories;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private MaterialCardView cardSuggestions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các view
        rvDiscoveryCategories = view.findViewById(R.id.rvDiscoveryCategories);
        etSearch = view.findViewById(R.id.etSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        cardSuggestions = view.findViewById(R.id.cardSuggestions);

        // 1. Khởi tạo RecyclerView với GridLayoutManager (2 cột)
        rvDiscoveryCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Cài đặt dữ liệu mẫu
        List<Category> categoryList = getMockCategories();
        DiscoveryCategoryAdapter adapter = new DiscoveryCategoryAdapter(categoryList);
        rvDiscoveryCategories.setAdapter(adapter);

        // 2. Xử lý Search: Lắng nghe sự kiện nhập liệu
        setupSearchLogic();
    }

    private List<Category> getMockCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("Sách nói", "1.2k+ đầu sách", R.drawable.bg_grad_audiobook, "Hot"));
        list.add(new Category("Ebook", "5k+ đầu sách", R.drawable.bg_grad_ebook, ""));
        list.add(new Category("Thiền & Chữa lành", "", R.drawable.bg_grad_meditation, "Hot"));
        list.add(new Category("Kinh doanh", "", R.drawable.bg_grad_business, "Mới nhất"));
        list.add(new Category("Tóm tắt sách", "15 phút nghe", R.drawable.bg_grad_summary, ""));
        list.add(new Category("Thiếu nhi", "", R.drawable.bg_grad_kids, "Truyện cổ tích"));
        return list;
    }

    private void setupSearchLogic() {
        // Ẩn nút xóa khi mới vào nếu chưa có text
        btnClearSearch.setVisibility(View.GONE);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Nếu có chữ: Hiện danh sách gợi ý và nút xóa
                    cardSuggestions.setVisibility(View.VISIBLE);
                    btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    // Nếu trống: Ẩn danh sách gợi ý và nút xóa
                    cardSuggestions.setVisibility(View.GONE);
                    btnClearSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Xử lý khi nhấn nút xóa (X)
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
        });
    }
}
