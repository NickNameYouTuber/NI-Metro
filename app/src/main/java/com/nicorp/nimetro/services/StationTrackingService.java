package com.nicorp.nimetro.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;

public class StationTrackingService extends Service {

    private static final String CHANNEL_ID = "StationTrackingChannel";
    private static final String ARRIVAL_CHANNEL_ID = "arrival_channel"; // Новый канал для уведомлений о прибытии
    private static final int NOTIFICATION_ID = 1;

    private Station currentStation;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        createArrivalNotificationChannel(); // Создаем канал для уведомлений о прибытии
    }

    private void createArrivalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel arrivalChannel = new NotificationChannel(
                    ARRIVAL_CHANNEL_ID,
                    "Arrival Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(arrivalChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("currentStation")) {
            currentStation = (Station) intent.getParcelableExtra("currentStation");
            updateNotification(currentStation);
        }

        startForeground(NOTIFICATION_ID, createNotification(currentStation));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Station Tracking Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification(Station station) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String stationName = station != null ? station.getName() : "Неизвестная станция";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Текущая станция")
                .setContentText(stationName)
                .setSmallIcon(R.drawable.ic_m_icon)
                .setContentIntent(pendingIntent)
                .build();
    }

    public void updateNotification(Station station) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(station));
        }
    }
}