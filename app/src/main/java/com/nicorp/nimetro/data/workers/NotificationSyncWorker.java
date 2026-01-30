package com.nicorp.nimetro.data.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.nicorp.nimetro.data.exceptions.ApiException;
import com.nicorp.nimetro.data.services.NotificationSyncService;

public class NotificationSyncWorker extends Worker {
    private static final String TAG = "NotificationSyncWorker";

    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            NotificationSyncService syncService = new NotificationSyncService(getApplicationContext());
            
            if (!syncService.isOnline()) {
                Log.d(TAG, "No internet connection, skipping notification sync");
                return Result.retry();
            }

            syncService.syncNotifications();
            Log.d(TAG, "Notification sync completed successfully");
            return Result.success();

        } catch (ApiException e) {
            Log.e(TAG, "Failed to sync notifications", e);
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Error syncing notifications", e);
            return Result.failure();
        }
    }
}

