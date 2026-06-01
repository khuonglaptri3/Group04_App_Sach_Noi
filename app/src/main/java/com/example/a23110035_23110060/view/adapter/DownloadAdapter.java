package com.example.a23110035_23110060.view.adapter;

import com.example.a23110035_23110060.R;

import com.example.a23110035_23110060.helper.DownloadHelper;
import com.example.a23110035_23110060.model.DownloadItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {
    private List<DownloadItem> downloadList;
    private OnDownloadDeleteListener listener;

    public interface OnDownloadDeleteListener {
        void onDelete(DownloadItem item, int position);
    }

    public DownloadAdapter(List<DownloadItem> downloadList, OnDownloadDeleteListener listener) {
        this.downloadList = downloadList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadItem item = downloadList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle());
        
        if ("audio".equals(item.getFileType())) {
            holder.tvType.setText("Sách nói");
            holder.ivTypeIcon.setImageResource(R.drawable.ic_headphones);
        } else {
            holder.tvType.setText("Ebook");
            holder.ivTypeIcon.setImageResource(R.drawable.ic_menu_book);
        }

        // Format size in MB
        double sizeMB = item.getSizeBytes() / (1024.0 * 1024.0);
        holder.tvSize.setText(String.format("%.1f MB", sizeMB));

        if (item.getCoverUrl() != null && !item.getCoverUrl().isEmpty()) {
            Glide.with(context)
                 .load(item.getCoverUrl())
                 .placeholder(R.drawable.bacl)
                 .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.bacl);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            File file = DownloadHelper.getDownloadedFile(context, item.getBookId(), item.getFileType());
            if (file != null && file.exists()) {
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "audio".equals(item.getFileType()) ? "audio/*" : "application/epub+zip");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    context.startActivity(android.content.Intent.createChooser(intent, "Mở bằng"));
                } catch (Exception e) {
                    android.widget.Toast.makeText(context, "Không tìm thấy ứng dụng để mở file này", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                android.widget.Toast.makeText(context, "File không tồn tại", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return downloadList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover, ivTypeIcon;
        TextView tvTitle, tvType, tvSize;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivDownloadCover);
            ivTypeIcon = itemView.findViewById(R.id.ivDownloadTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvDownloadTitle);
            tvType = itemView.findViewById(R.id.tvDownloadType);
            tvSize = itemView.findViewById(R.id.tvDownloadSize);
            btnDelete = itemView.findViewById(R.id.btnDeleteDownload);
        }
    }
}
