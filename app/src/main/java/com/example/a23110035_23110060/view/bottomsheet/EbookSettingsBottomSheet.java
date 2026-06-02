package com.example.a23110035_23110060.view.bottomsheet;

import com.example.a23110035_23110060.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EbookSettingsBottomSheet extends BottomSheetDialogFragment {

    public interface SettingsListener {
        void onBrightnessChanged(int progress);
        void onFontSizeAdjusted(int delta);
        void onThemeChanged(String bgColor, String textColor);
    }

    private SettingsListener listener;
    private float currentTextSize;

    public EbookSettingsBottomSheet(SettingsListener listener, float currentTextSize) {
        this.listener = listener;
        this.currentTextSize = currentTextSize;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ebook_settings_sheet, container, false);

        SeekBar seekBrightness = view.findViewById(R.id.seek_brightness);
        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (listener != null) listener.onBrightnessChanged(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        TextView tvFontSize = view.findViewById(R.id.tv_font_size);
        tvFontSize.setText(currentTextSize + "px");

        view.findViewById(R.id.btn_font_decrease).setOnClickListener(v -> {
            if (listener != null) {
                currentTextSize = Math.max(14, currentTextSize - 2);
                tvFontSize.setText(currentTextSize + "px");
                listener.onFontSizeAdjusted(-2);
            }
        });

        view.findViewById(R.id.btn_font_increase).setOnClickListener(v -> {
            if (listener != null) {
                currentTextSize = Math.min(28, currentTextSize + 2);
                tvFontSize.setText(currentTextSize + "px");
                listener.onFontSizeAdjusted(2);
            }
        });

        view.findViewById(R.id.btn_theme_light).setOnClickListener(v -> {
            if (listener != null) listener.onThemeChanged("#FFFFFF", "#1D1B20");
        });
        view.findViewById(R.id.btn_theme_sepia).setOnClickListener(v -> {
            if (listener != null) listener.onThemeChanged("#F5E6C8", "#5D4037");
        });
        view.findViewById(R.id.btn_theme_dark).setOnClickListener(v -> {
            if (listener != null) listener.onThemeChanged("#1A1A2E", "#E0E0E0");
        });

        return view;
    }
}
