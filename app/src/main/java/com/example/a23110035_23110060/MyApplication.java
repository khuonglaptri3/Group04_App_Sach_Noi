package com.example.a23110035_23110060;

import com.example.a23110035_23110060.service.SyncWorker;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        setupBackgroundSync();
    }

    private void setupBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "BookSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
