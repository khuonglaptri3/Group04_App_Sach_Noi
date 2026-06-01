package com.example.a23110035_23110060.helper;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;

public class DownloadHelper {

    public static void downloadFile(Context context, String url, String bookId, String title, String fileType) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "Link tải không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(title);
            request.setDescription("Đang tải xuống " + (fileType.equals("audio") ? "Sách nói" : "Ebook"));
            
            // Show notification while downloading and after completion
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // Allow download over mobile and wifi
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            // Determine file extension
            String extension = fileType.equals("audio") ? ".mp3" : ".epub";
            String fileName = bookId + "_" + fileType + extension;

            // Save to app-specific external storage (no special permissions required on modern Android)
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(context, "Bắt đầu tải xuống...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi tải xuống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    public static File getDownloadedFile(Context context, String bookId, String fileType) {
        String extension = fileType.equals("audio") ? ".mp3" : ".epub";
        String fileName = bookId + "_" + fileType + extension;
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir != null) {
            File file = new File(dir, fileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
    
    public static boolean deleteDownloadedFile(Context context, String bookId, String fileType) {
        File file = getDownloadedFile(context, bookId, fileType);
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }
}
