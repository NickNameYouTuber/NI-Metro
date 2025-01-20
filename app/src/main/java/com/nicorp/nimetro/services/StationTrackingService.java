package com.nicorp.nimetro.services;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;

import java.util.List;

public class StationTrackingService extends Service {

    private static final String CHANNEL_ID = "StationTrackingChannel";
    private static final String ARRIVAL_CHANNEL_ID = "arrival_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long GPS_TIMEOUT = 30000; // 30 секунд
    private static boolean isRunning = false;

    private Station currentStation;
    private Station previousStation;
    private List<Station> route;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler = new Handler();
    private Runnable locationUpdateRunnable;

    // Новые поля для отслеживания времени
    private long lastLocationTime = 0;
    private boolean isTimerMode = false;
    private long timerStartTime = 0;
    private int currentStationIndex = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        if (isRunning) {
            stopSelf();
            return;
        }
        isRunning = true;
        createNotificationChannel();
        createArrivalNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates();
                checkLocationTimeout();
                handler.postDelayed(this, 10000);
            }
        };

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerMode) {
                    updateStationByTime();
                }
            }
        };

        handler.post(locationUpdateRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(locationUpdateRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("route")) {
            route = intent.getParcelableArrayListExtra("route");
            currentStation = intent.getParcelableExtra("currentStation");

            // Найти индекс текущей станции в маршруте
            currentStationIndex = findStationIndex(currentStation);

            if (previousStation == null || !currentStation.equals(previousStation)) {
                updateNotification(currentStation);
                previousStation = currentStation;
            }
        }

        startForeground(NOTIFICATION_ID, createNotification(currentStation));
        return START_STICKY;
    }

    private void checkLocationTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLocationTime > GPS_TIMEOUT && !isTimerMode) {
            // Переключаемся на режим таймера
            switchToTimerMode();
        }
    }

    private void switchToTimerMode() {
        isTimerMode = true;
        timerStartTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
//        Log.d("StationTrackingService", "Switched to timer mode");
    }

    private void switchToGpsMode() {
        isTimerMode = false;
        timerHandler.removeCallbacks(timerRunnable);
//        Log.d("StationTrackingService", "Switched back to GPS mode");
    }

    private void updateStationByTime() {
        if (currentStation == null || route == null || route.isEmpty()) {
            return;
        }

        // Находим следующую станцию в маршруте
        Station nextStation = findNextStation();
        if (nextStation == null) {
            return;
        }

        // Получаем время в пути до следующей станции
        float travelTime = getTravelTime(currentStation, nextStation) - 0.5f;
        long elapsedTime = System.currentTimeMillis() - timerStartTime;

        // Если прошло достаточно времени, переходим к следующей станции
        if (elapsedTime >= travelTime * 60 * 1000) { // переводим время в миллисекунды
            currentStation = nextStation;
            currentStationIndex = findStationIndex(currentStation);
            updateNotification(currentStation);

            // Обновляем время начала для следующего интервала
            timerStartTime = System.currentTimeMillis();

            // Отправляем broadcast с обновлением
            sendStationUpdateBroadcast();
        }

        // Планируем следующую проверку
        timerHandler.postDelayed(timerRunnable, 1000); // проверяем каждую секунду
    }

    private Station findNextStation() {
        if (currentStationIndex < route.size() - 1) {
            return route.get(currentStationIndex + 1);
        }
        return null;
    }

    private int getTravelTime(Station current, Station next) {
        for (Station.Neighbor neighbor : current.getNeighbors()) {
//            Log.d("StationTrackingService", "Neighbor: " + neighbor.getStation().getId());
            if (neighbor.getStation().getId().equals(next.getId())) {
//                Log.d("StationTrackingService", "Found neighbor: " + neighbor.getStation().getId());
                return neighbor.getTime();
            }
        }
        return 180; // возвращаем значение по умолчанию, если время не найдено
    }

    private int findStationIndex(Station station) {
        for (int i = 0; i < route.size(); i++) {
//            Log.d("StationTrackingService", "Station ne: " + route.get(i).getNeighbors().get(0).getStation().getName());
            if (route.get(i).getId().equals(station.getId())) {
                return i;
            }
        }
        return 0;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    lastLocationTime = System.currentTimeMillis();
                    if (isTimerMode) {
                        switchToGpsMode();
                    }
                    updateUserPosition(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void updateUserPosition(Location location) {
        if (route == null || route.isEmpty()) {
            return;
        }

        Station nearestStation = findNearestStation(location.getLatitude(), location.getLongitude());
        if (nearestStation != null && !nearestStation.equals(currentStation)) {
            currentStation = nearestStation;
            currentStationIndex = findStationIndex(currentStation);
            updateNotification(currentStation);
            sendStationUpdateBroadcast();
        }
    }

    private void sendStationUpdateBroadcast() {
        Intent intent = new Intent("com.nicorp.nimetro.UPDATE_STATION");
        intent.putExtra("currentStation", currentStation);
        sendBroadcast(intent);
//        Log.d("StationTrackingService", "Broadcast sent for station: " + currentStation.getName());
    }

    private Station findNearestStation(double latitude, double longitude) {
        Station nearestStation = null;
        double minDistance = Double.MAX_VALUE;

        for (Station station : route) {
            double distance = station.distanceTo(latitude, longitude);
            if (distance < minDistance) {
                minDistance = distance;
                nearestStation = station;
            }
        }

        return nearestStation;
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
                    NotificationManager.IMPORTANCE_LOW
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
        String trackingMode = isTimerMode ? " (по времени)" : " (по GPS)";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Текущая станция")
                .setContentText(stationName + trackingMode)
                .setSmallIcon(R.drawable.ic_m_icon)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(Station station) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(station));
        }
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
}