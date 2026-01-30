package com.nicorp.nimetro.data.workers;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NotificationSyncWorkManager {
    private static final String TAG = "NotificationSyncWorkManager";
    private static final String WORK_NAME = "notification_sync_work";
    private static final long SYNC_INTERVAL_HOURS = 1;

    public static void startPeriodicSync(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationSyncWorker.class,
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            );

            Log.d(TAG, "Periodic notification sync started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start periodic sync", e);
        }
    }

    public static void stopPeriodicSync(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            Log.d(TAG, "Periodic notification sync stopped");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop periodic sync", e);
        }
    }
}

