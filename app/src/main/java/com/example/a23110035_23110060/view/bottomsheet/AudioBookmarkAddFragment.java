package com.example.a23110035_23110060.view.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;
import com.example.a23110035_23110060.controller.PlayerManager;
import com.example.a23110035_23110060.controller.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioBookmarkAddFragment extends BottomSheetDialogFragment {

    private String bookId;
    private int positionSeconds;

    public AudioBookmarkAddFragment() {}

    public static AudioBookmarkAddFragment newInstance(String bookId, int positionSeconds) {
        AudioBookmarkAddFragment fragment = new AudioBookmarkAddFragment();
        Bundle args = new Bundle();
        args.putString("bookId", bookId);
        args.putInt("positionSeconds", positionSeconds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString("bookId");
            positionSeconds = getArguments().getInt("positionSeconds");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_bookmark_add, container, false);
        
        TextView tvSubtitle = view.findViewById(R.id.tv_bookmark_subtitle);
        android.widget.EditText etNote = view.findViewById(R.id.et_bookmark_note);
        android.widget.Button btnSave = view.findViewById(R.id.btn_save);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_cancel);

        int minutes = positionSeconds / 60;
        int seconds = positionSeconds % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        
        PlayerManager pm = PlayerManager.getInstance();
        String chapterTitle = "ĐÁNH DẤU";
        if (pm.getCurrentChapterTitle() != null && !pm.getCurrentChapterTitle().isEmpty()) {
            chapterTitle = pm.getCurrentChapterTitle().toUpperCase();
        }
        
        tvSubtitle.setText(chapterTitle + " - Trích đoạn " + timeStr);

        btnSave.setOnClickListener(v -> {
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
            saveBookmark(note);
        });
        
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    private void saveBookmark(String note) {
        SessionManager sessionManager = new SessionManager(requireContext());
        String userId = sessionManager.getUserId();
        
        if (userId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookmarks";
        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("book_id", bookId);
            json.put("position_seconds", positionSeconds);
            json.put("note", note.isEmpty() ? "Đánh dấu tại " + String.format(Locale.getDefault(), "%02d:%02d", positionSeconds / 60, positionSeconds % 60) : note);
        } catch (Exception e) {}

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .build();

        com.example.a23110035_23110060.controller.NetworkClient.getClient(requireContext()).newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đã thêm đánh dấu", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}
