package com.example.a23110035_23110060.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncWorker", "Bắt đầu đồng bộ hóa dữ liệu ngầm...");
        
        try {
            // Giả lập quá trình đồng bộ tiến trình đọc sách lên server
            Thread.sleep(2000);
            
            Log.d("SyncWorker", "Đồng bộ hóa hoàn tất!");
            return Result.success();
        } catch (Exception e) {
            Log.e("SyncWorker", "Lỗi đồng bộ", e);
            return Result.failure();
        }
    }
}
