package com.example.a23110035_23110060.view.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.a23110035_23110060.R;

public class TimerBottomSheetFragment extends BottomSheetDialogFragment {
    public interface TimerListener {
        void onTimerSelected(int minutes);
    }

    private final TimerListener listener;
    private final int[] times = {0, 5, 15, 30, 45, 60, -1};
    private final String[] titles = {"Không hẹn giờ", "5 phút", "15 phút", "30 phút", "45 phút", "60 phút", "Hết chương"};
    private int selectedIndex = 0;
    
    private final java.util.List<android.widget.ImageView> radioIcons = new java.util.ArrayList<>();

    public TimerBottomSheetFragment(TimerListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer_bottom_sheet, container, false);
        LinearLayout containerLayout = view.findViewById(R.id.ll_timer_container);
        
        for (int i = 0; i < times.length; i++) {
            View itemView = inflater.inflate(R.layout.item_timer_option, containerLayout, false);
            TextView tvTitle = itemView.findViewById(R.id.tv_title);
            android.widget.ImageView ivRadio = itemView.findViewById(R.id.iv_radio);
            
            tvTitle.setText(titles[i]);
            radioIcons.add(ivRadio);
            
            final int index = i;
            itemView.findViewById(R.id.ll_option).setOnClickListener(v -> {
                selectedIndex = index;
                updateSelection();
            });
            
            containerLayout.addView(itemView);
        }
        
        updateSelection();
        
        view.findViewById(R.id.btn_done).setOnClickListener(v -> {
            if (listener != null) listener.onTimerSelected(times[selectedIndex]);
            dismiss();
        });

        return view;
    }
    
    private void updateSelection() {
        for (int i = 0; i < radioIcons.size(); i++) {
            if (i == selectedIndex) {
                radioIcons.get(i).setImageResource(R.drawable.ic_radio_selected);
            } else {
                radioIcons.get(i).setImageResource(R.drawable.ic_radio_unselected);
            }
        }
    }
}
