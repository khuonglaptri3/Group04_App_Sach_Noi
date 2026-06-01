package com.example.a23110035_23110060.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

public class CleanupService extends Service {
    private static final String TAG = "CleanupService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CleanupService started");

        new Thread(() -> {
            long deletedSize = 0;
            try {
                // Xóa bộ nhớ đệm (Cache) của ứng dụng
                File cacheDir = getCacheDir();
                deletedSize += deleteDir(cacheDir);

                File externalCacheDir = getExternalCacheDir();
                if (externalCacheDir != null) {
                    deletedSize += deleteDir(externalCacheDir);
                }

                Log.d(TAG, "CleanupService completed. Freed: " + (deletedSize / 1024) + " KB");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning cache", e);
            } finally {
                stopSelf(); // Tự động tắt service sau khi dọn dẹp xong
            }
        }).start();

        return START_NOT_STICKY;
    }

    private long deleteDir(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    File childFile = new File(dir, child);
                    if (childFile.isDirectory()) {
                        size += deleteDir(childFile);
                    } else {
                        size += childFile.length();
                        childFile.delete();
                    }
                }
            }
        }
        return size;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
