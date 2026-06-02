package com.example.a23110035_23110060.view.fragment;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.view.activity.EbookReaderActivity;

import com.example.a23110035_23110060.controller.SessionManager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookNotesFragment extends BottomSheetDialogFragment {

    private static final String TAG = "BookNotesFragment";
    private String bookId;
    private SessionManager sessionManager;
    private OkHttpClient client = new OkHttpClient();
    
    private RecyclerView rvNotes;
    private EditText etNoteInput;
    private MaterialButton btnSaveNote;
    private TextView tvEmptyNotes;
    
    private List<BookNote> notesList = new ArrayList<>();
    private NotesAdapter adapter;
    
    private String editingNoteId = null;

    public static BookNotesFragment newInstance(String bookId) {
        BookNotesFragment fragment = new BookNotesFragment();
        Bundle args = new Bundle();
        args.putString("bookId", bookId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_book_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookId = getArguments() != null ? getArguments().getString("bookId") : null;
        sessionManager = new SessionManager(requireContext());

        rvNotes = view.findViewById(R.id.rv_notes);
        etNoteInput = view.findViewById(R.id.et_note_input);
        btnSaveNote = view.findViewById(R.id.btn_save_note);
        tvEmptyNotes = view.findViewById(R.id.tv_empty_notes);

        ImageButton btnClose = view.findViewById(R.id.btn_close_dialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        rvNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotesAdapter(notesList);
        rvNotes.setAdapter(adapter);

        btnSaveNote.setOnClickListener(v -> performSaveNote());

        fetchBookNotes();
    }

    private void fetchBookNotes() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null || bookId == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_notes?user_id=eq." + userId + 
                     "&book_id=eq." + bookId + "&select=id,note,created_at,book_highlights(page_number,highlighted_text)&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Fetch notes failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        notesList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            String id = obj.getString("id");
                            String note = obj.optString("note", "");
                            String createdAt = obj.optString("created_at", "");
                            
                            int page = 1;
                            String highlightedText = "";
                            JSONObject highlightObj = obj.optJSONObject("book_highlights");
                            if (highlightObj != null) {
                                page = highlightObj.optInt("page_number", 1);
                                highlightedText = highlightObj.optString("highlighted_text", "");
                            }
                            
                            notesList.add(new BookNote(id, note, createdAt, page, highlightedText));
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                tvEmptyNotes.setVisibility(notesList.isEmpty() ? View.VISIBLE : View.GONE);
                                rvNotes.setVisibility(notesList.isEmpty() ? View.GONE : View.VISIBLE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse notes failed", e);
                    }
                }
            }
        });
    }

    private void performSaveNote() {
        String noteText = etNoteInput.getText().toString().trim();
        if (noteText.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null || bookId == null) return;

        btnSaveNote.setEnabled(false);

        if (editingNoteId != null) {
            // Update mode (PATCH)
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_notes?id=eq." + editingNoteId;
            JSONObject json = new JSONObject();
            try {
                json.put("note", noteText);
            } catch (Exception e) {}

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .patch(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    resetInputStateAndRefresh(false, "Sửa thất bại");
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    resetInputStateAndRefresh(response.isSuccessful(), response.isSuccessful() ? "Đã sửa ghi chú" : "Sửa thất bại");
                }
            });
        } else {
            btnSaveNote.setEnabled(true);
            Toast.makeText(getContext(), "Chỉ có thể tạo ghi chú bằng cách chọn văn bản trong sách", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteNote(String id) {
        String token = sessionManager.getAccessToken();
        if (token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/book_notes?id=eq." + id;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                            fetchBookNotes();
                        } else {
                            Toast.makeText(getContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void resetInputStateAndRefresh(boolean success, String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                btnSaveNote.setEnabled(true);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                if (success) {
                    etNoteInput.setText("");
                    editingNoteId = null;
                    btnSaveNote.setText("Lưu");
                    fetchBookNotes();
                }
            });
        }
    }

    // Static inner classes for representation
    public static class BookNote {
        public String id;
        public String note;
        public String createdAt;
        public int pageNumber;
        public String highlightedText;

        public BookNote(String id, String note, String createdAt, int pageNumber, String highlightedText) {
            this.id = id;
            this.note = note;
            this.createdAt = createdAt;
            this.pageNumber = pageNumber;
            this.highlightedText = highlightedText;
        }
    }

    // RecyclerView Adapter
    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
        private final List<BookNote> list;

        public NotesAdapter(List<BookNote> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_note, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookNote note = list.get(position);
            holder.tvText.setText(note.note);
            
            if (note.highlightedText != null && !note.highlightedText.isEmpty()) {
                holder.tvHighlightedText.setVisibility(View.VISIBLE);
                holder.tvHighlightedText.setText("\"" + note.highlightedText + "\"");
            } else {
                holder.tvHighlightedText.setVisibility(View.GONE);
            }
            
            // Format page number and date
            String pageText = "Trang " + note.pageNumber;
            String dateText = formatSupabaseDate(note.createdAt);
            holder.tvDate.setText(pageText + "  •  " + dateText);

            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof EbookReaderActivity) {
                    ((EbookReaderActivity) getActivity()).jumpToPage(note.pageNumber - 1);
                    dismiss();
                }
            });

            holder.btnEdit.setOnClickListener(v -> {
                editingNoteId = note.id;
                etNoteInput.setText(note.note);
                etNoteInput.requestFocus();
                btnSaveNote.setText("Cập nhật");
            });

            holder.btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Xóa ghi chú")
                    .setMessage("Bạn có chắc muốn xóa ghi chú này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteNote(note.id))
                    .setNegativeButton("Hủy", null)
                    .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private String formatSupabaseDate(String rawDate) {
            if (rawDate == null || rawDate.isEmpty() || rawDate.equals("null")) return "";
            try {
                String formatted = rawDate.replace("Z", "+00:00");
                if (formatted.contains(".")) {
                    int dotIdx = formatted.indexOf('.');
                    int plusIdx = formatted.indexOf('+', dotIdx);
                    if (plusIdx != -1) {
                        formatted = formatted.substring(0, dotIdx) + formatted.substring(plusIdx);
                    } else {
                        formatted = formatted.substring(0, dotIdx);
                    }
                }
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = parser.parse(formatted);
                if (date != null) {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    formatter.setTimeZone(TimeZone.getDefault());
                    return formatter.format(date);
                }
            } catch (Exception e) {
                Log.e(TAG, "Date parsing error", e);
            }
            return rawDate;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText, tvDate, tvHighlightedText;
            ImageButton btnEdit, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tv_note_text);
                tvHighlightedText = itemView.findViewById(R.id.tv_highlighted_text);
                tvDate = itemView.findViewById(R.id.tv_note_date);
                btnEdit = itemView.findViewById(R.id.btn_edit_note);
                btnDelete = itemView.findViewById(R.id.btn_delete_note);
            }
        }
    }
}
