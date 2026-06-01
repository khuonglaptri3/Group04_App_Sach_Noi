package com.example.a23110035_23110060.view.bottomsheet;

import com.example.a23110035_23110060.BuildConfig;
import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.controller.SessionManager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private OkHttpClient client = new OkHttpClient();
    private SessionManager sessionManager;
    private RecyclerView rvNotifications;
    private NotificationsAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(requireContext());
        
        rvNotifications = view.findViewById(R.id.rv_notifications);
        tvEmpty = view.findViewById(R.id.tv_empty);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new NotificationsAdapter();
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);

        view.findViewById(R.id.tv_mark_all_read).setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void fetchNotifications() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        if (userId == null || token == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?user_id=eq." + userId + "&order=created_at.desc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Lỗi tải thông báo");
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        notifications.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            notifications.add(new Notification(
                                    obj.getString("id"),
                                    obj.getString("type"),
                                    obj.optJSONObject("data"),
                                    obj.getBoolean("is_read"),
                                    obj.getString("created_at")
                            ));
                        }
                        
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Notifications", "Parse error", e);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                tvEmpty.setVisibility(View.VISIBLE);
                            });
                        }
                    }
                }
            }
        });
    }

    private void markAllAsRead() {
        String token = sessionManager.getAccessToken();
        if (token == null) return;

        String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/mark_all_notifications_as_read";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .post(okhttp3.RequestBody.create("", okhttp3.MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded()) {
                    requireActivity().runOnUiThread(() -> fetchNotifications());
                }
            }
        });
    }

    private class Notification {
        String id, type, createdAt;
        JSONObject data;
        boolean isRead;

        Notification(String id, String type, JSONObject data, boolean isRead, String createdAt) {
            this.id = id;
            this.type = type;
            this.data = data;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }

    private class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notification item = notifications.get(position);
            holder.vUnreadDot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
            
            String title = "Thông báo";
            String msg = "Bạn có một thông báo mới.";
            
            switch (item.type) {
                case "payment_success":
                    title = "Thanh toán thành công";
                    msg = "Giao dịch mua sách đã thành công.";
                    holder.ivIcon.setImageResource(R.drawable.ic_verified);
                    break;
                case "payment_failed":
                    title = "Thanh toán thất bại";
                    msg = "Giao dịch không thành công. Vui lòng thử lại.";
                    holder.ivIcon.setImageResource(R.drawable.ic_close);
                    break;
                case "premium_activated":
                    title = "Kích hoạt Premium";
                    msg = "Bạn đã kích hoạt thành công gói Premium!";
                    holder.ivIcon.setImageResource(R.drawable.ic_star);
                    break;
                case "book_unlocked":
                    title = "Đã mở khóa sách";
                    msg = "Cuốn sách đã được thêm vào thư viện của bạn.";
                    holder.ivIcon.setImageResource(R.drawable.ic_library);
                    break;
                case "reminder":
                    title = "Nhắc nhở";
                    msg = "Đã đến giờ đọc sách!";
                    holder.ivIcon.setImageResource(R.drawable.ic_bell);
                    break;
                default:
                    holder.ivIcon.setImageResource(R.drawable.ic_bell);
                    break;
            }
            
            if (item.data != null) {
                if (item.data.has("title")) title = item.data.optString("title", title);
                if (item.data.has("message")) msg = item.data.optString("message", msg);
            }

            holder.tvTitle.setText(title);
            holder.tvMessage.setText(msg);
            
            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = parser.parse(item.createdAt.split("\\.")[0]);
                if (date != null) {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    holder.tvTime.setText(formatter.format(date));
                }
            } catch (Exception e) {
                holder.tvTime.setText("");
            }

            holder.itemView.setOnClickListener(v -> {
                if (!item.isRead) {
                    markAsRead(item.id);
                    item.isRead = true;
                    notifyItemChanged(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        private void markAsRead(String id) {
            String token = sessionManager.getAccessToken();
            if (token == null) return;

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/rpc/mark_notification_as_read";
            String json = "{\"notification_uuid\":\"" + id + "\"}";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(okhttp3.RequestBody.create(json, okhttp3.MediaType.parse("application/json")))
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {}
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle, tvMessage, tvTime;
            View vUnreadDot;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_icon);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvMessage = itemView.findViewById(R.id.tv_message);
                tvTime = itemView.findViewById(R.id.tv_time);
                vUnreadDot = itemView.findViewById(R.id.v_unread_dot);
            }
        }
    }
}
