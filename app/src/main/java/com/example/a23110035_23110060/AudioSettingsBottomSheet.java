package com.example.a23110035_23110060;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;

public class AudioSettingsBottomSheet extends BottomSheetDialogFragment {

    public interface AudioSettingsListener {
        void onSpeedChanged(float speed);
        void onTimerSet(int minutes);
    }

    private AudioSettingsListener listener;
    private float currentSpeed;

    public AudioSettingsBottomSheet(AudioSettingsListener listener, float currentSpeed) {
        this.listener = listener;
        this.currentSpeed = currentSpeed;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_settings_sheet, container, false);

        Slider sliderSpeed = view.findViewById(R.id.slider_speed);
        sliderSpeed.setValue(currentSpeed);
        sliderSpeed.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && listener != null) {
                listener.onSpeedChanged(value);
            }
        });

        setupTimerChips(view);

        view.findViewById(R.id.chip_timer_off).setOnClickListener(v -> setTimer(0));
        view.findViewById(R.id.chip_timer_15).setOnClickListener(v -> setTimer(15));
        view.findViewById(R.id.chip_timer_30).setOnClickListener(v -> setTimer(30));
        view.findViewById(R.id.chip_timer_45).setOnClickListener(v -> setTimer(45));
        view.findViewById(R.id.chip_timer_60).setOnClickListener(v -> setTimer(60));

        return view;
    }

    private void setupTimerChips(View view) {
        // Just visual setup if needed
    }

    private void setTimer(int minutes) {
        if (listener != null) {
            listener.onTimerSet(minutes);
        }
        if (minutes == 0) {
            Toast.makeText(getContext(), "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Sẽ tắt sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }
}
