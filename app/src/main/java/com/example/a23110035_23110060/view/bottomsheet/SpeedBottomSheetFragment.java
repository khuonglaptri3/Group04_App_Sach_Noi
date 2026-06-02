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
import java.util.Locale;

public class SpeedBottomSheetFragment extends BottomSheetDialogFragment {
    public interface SpeedListener {
        void onSpeedSelected(float speed);
    }

    private final float currentSpeed;
    private final SpeedListener listener;

    public SpeedBottomSheetFragment(float currentSpeed, SpeedListener listener) {
        this.currentSpeed = currentSpeed;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speed_bottom_sheet, container, false);
        
        TextView tvSpeedTitle = view.findViewById(R.id.tv_speed_title);
        com.google.android.material.slider.Slider slider = view.findViewById(R.id.slider_speed);
        
        tvSpeedTitle.setText(String.format(java.util.Locale.getDefault(), "Tốc độ phát %.1fx", currentSpeed));
        slider.setValue(currentSpeed);
        
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            tvSpeedTitle.setText(String.format(java.util.Locale.getDefault(), "Tốc độ phát %.1fx", value));
        });
        
        slider.addOnSliderTouchListener(new com.google.android.material.slider.Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {
                if (listener != null) listener.onSpeedSelected(slider.getValue());
                // We don't dismiss immediately so they can adjust and hear it
            }
        });
        
        return view;
    }
}
