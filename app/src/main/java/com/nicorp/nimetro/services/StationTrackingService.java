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
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
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
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StationTrackingService extends Service {

    private static final String CHANNEL_ID = "StationTrackingChannel";
    private static final String ARRIVAL_CHANNEL_ID = "arrival_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long GPS_TIMEOUT = 25000; // 45 секунд
    private static final float MAX_ACCEPTABLE_ACCURACY = 10000.0f; // Максимальная допустимая погрешность (метры)
    private static final long POOR_SIGNAL_DURATION_THRESHOLD = 60000; // 1 минута плохого сигнала
    private static final float MIN_MOVEMENT_DISTANCE = 150.0f; // Минимальное движение для определения стагнации
    private static final long MIN_STATION_TIME = 90000; // 90 секунд минимальное время между станциями
    private static boolean isRunning = false;

    private Station currentStation;
    private Station previousStation;
    private List<Station> route;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler = new Handler();
    private Runnable locationUpdateRunnable;

    private long lastLocationTime = 0;
    private boolean isTimerMode = false;
    private long timerStartTime = 0;
    private int currentStationIndex = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private long poorSignalStartTime = 0;
    private List<Location> lastLocations = new ArrayList<>();
    private boolean gpsEnabled = false;
    private long lastStationUpdateTime = 0;
    private boolean isFirstUpdate = true;
    private LocationManager locationManager;
    private List<Line> lines;

    private TextToSpeech textToSpeech;

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
        lastLocationTime = System.currentTimeMillis();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        checkGpsStatus();

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

        // Инициализация TextToSpeech
        initTextToSpeech();
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("ru"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Русский язык не поддерживается");
                }
            } else {
                Log.e("TTS", "Ошибка инициализации TextToSpeech");
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(locationUpdateRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("route") && intent.getParcelableArrayListExtra("route") != null) {
                route = intent.getParcelableArrayListExtra("route");
                Log.d("StationTracking", "Route received with size: " + route.size());
            }

            if (intent.hasExtra("currentStation")) {
                currentStation = intent.getParcelableExtra("currentStation");
                if (currentStation != null) {
                    currentStationIndex = findStationIndex(currentStation);
                    Log.d("StationTracking", "Initial station: " + currentStation.getName()
                            + " at index: " + currentStationIndex);
                }
            }
        }

        if (intent != null && intent.hasExtra("route")) {
            route = intent.getParcelableArrayListExtra("route");
            lines = intent.getParcelableArrayListExtra("lines");
            currentStation = intent.getParcelableExtra("currentStation");

            currentStationIndex = findStationIndex(currentStation);
            Log.d("StationTrackingService", "Current station index: " + currentStationIndex);
            Log.d("StationTrackingService", "Current station: " + currentStation.getName());

            if (previousStation == null || !currentStation.equals(previousStation)) {
                updateNotification(currentStation);
                previousStation = currentStation;
            }

            isFirstUpdate = true;
        }

        startForeground(NOTIFICATION_ID, createNotification(currentStation));
        return START_STICKY;
    }

    private void checkLocationTimeout() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastLocationTime) > GPS_TIMEOUT
                && !isTimerMode
                && currentStationIndex < route.size() - 1) {
            switchToTimerMode();
        }
    }

    private void switchToTimerMode() {
        isTimerMode = true;
        timerStartTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        Log.d("StationTrackingService", "Switched to timer mode");
    }

    private void switchToGpsMode() {
        if (isTimerMode) {
            isTimerMode = false;
            timerHandler.removeCallbacks(timerRunnable);
            Log.d("StationTrackingService", "Switched back to GPS mode");

            lastStationUpdateTime = 0;

            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        updateUserPosition(location);
                        updateNotification(currentStation);
                    }
                });
            }
        }
    }

    private void updateStationByTime() {
        if (currentStation == null || route == null || route.isEmpty()) return;

        Station nextStation = findNextStation();
        if (nextStation == null) return;

        float travelTime = getTravelTime(currentStation, nextStation) - ((float) GPS_TIMEOUT / 1000.0f / 60.0f);
        long elapsedTime = System.currentTimeMillis() - timerStartTime;

        if (elapsedTime >= travelTime * 60 * 1000) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastStationUpdateTime >= MIN_STATION_TIME) {
                Station previousStation = currentStation;
                currentStation = nextStation;
                currentStationIndex = findStationIndex(currentStation);
                lastStationUpdateTime = currentTime;

                // Проверяем, нужно ли перейти на следующую станцию
                checkAndSwitchToNextStation();

                updateNotification(currentStation);
                Log.d("StationTrackingService", "Updated to next station by time: " + currentStation.getName());
                sendStationUpdateBroadcast();

                checkAndAnnounceArrival();
            }
            timerStartTime = System.currentTimeMillis();
        }

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void checkAndAnnounceTransfers(Station previousStation) {
        if (currentStationIndex + 1 < route.size()) {
            Station nextStation = route.get(currentStationIndex + 1);
            String previousLine = getLineForStation(previousStation);
            String nextLine = getLineForStation(nextStation);

            if (!previousLine.equals(nextLine)) {
                announceTransfer(nextStation.getName(), nextLine);
            } else if (currentStationIndex + 2 < route.size()) {
                nextStation = route.get(currentStationIndex + 2);
                String currentLine = getLineForStation(currentStation);
                nextLine = getLineForStation(nextStation);

                if (!currentLine.equals(nextLine)) {
                    announceUpcomingTransfer(nextStation, nextStation, nextLine);
                }
            }
        }

    }

    private String getLineForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().stream().anyMatch(s -> s.getId().equals(station.getId()))) {
                return line.getName();
            }
        }
        return "";
    }

    private void announceTransfer(String stationName, String newLineName) {
        String message = "Перейдите на станцию " + stationName + " " + formatLineName(newLineName);
        speak(message);
    }

    private void announceUpcomingTransfer(Station transferStation, Station nextStation, String newLineName) {
        String message = String.format("Следующая станция: %s. Приготовьтесь к переходу на станцию %s %s",
                transferStation.getName(),
                nextStation.getName(),
                formatLineName(newLineName));
        speak(message);
    }

    private String formatLineName(String lineName) {
        return lineName.replace("ая линия", "ой линии")
                .replace("линия", "линии");
    }

    private void checkAndAnnounceArrival() {
        if (currentStationIndex == route.size() - 2) {
            announceUpcomingArrival();
        }

        if (currentStationIndex == route.size() - 1) {
            announceFinalArrival();
        }
    }

    private void announceUpcomingArrival() {
        speak("На следующей станции конец маршрута");
    }

    private void announceFinalArrival() {
        speak("Вы прибыли в пункт назначения");
    }

    private Station findNextStation() {
        if (currentStationIndex < route.size() - 1) {
            return route.get(currentStationIndex + 1);
        }
        return null;
    }

    private int getTravelTime(Station current, Station next) {
        for (Station.Neighbor neighbor : current.getNeighbors()) {
            if (neighbor.getStation().getId().equals(next.getId())) {
                return neighbor.getTime();
            }
        }
        return 180;
    }

    private int findStationIndex(Station station) {
        for (int i = 0; i < route.size(); i++) {
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

        checkGpsStatus();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(5000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                checkGpsStatus();

                for (Location location : locationResult.getLocations()) {
                    lastLocationTime = System.currentTimeMillis();
                    boolean isGpsLocation = LocationManager.GPS_PROVIDER.equals(location.getProvider());
                    updateLocationHistory(location);

                    boolean isAccuracyPoor = location.getAccuracy() > MAX_ACCEPTABLE_ACCURACY;
                    boolean isLocationStagnant = checkLocationStagnation();

                    boolean hasGpsIssue = !gpsEnabled || (isGpsLocation && (isAccuracyPoor || isLocationStagnant));

                    if (hasGpsIssue) {
                        handlePotentialSignalIssue();
                    } else {
                        resetSignalChecks();
                        if (isTimerMode) {
                            switchToGpsMode();
                        }
                    }

                    if (!isTimerMode) {
                        updateUserPosition(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void checkGpsStatus() {
        gpsEnabled = locationManager != null &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void updateLocationHistory(Location location) {
        lastLocations.add(location);
        if (lastLocations.size() > 5) {
            lastLocations.remove(0);
        }
    }

    private boolean checkLocationStagnation() {
        return false;
    }

    private void handlePotentialSignalIssue() {
        if (poorSignalStartTime == 0) {
            poorSignalStartTime = System.currentTimeMillis();
            return;
        }

        long poorDuration = System.currentTimeMillis() - poorSignalStartTime;

        if (poorDuration >= POOR_SIGNAL_DURATION_THRESHOLD
                && !isTimerMode
                && currentStationIndex < route.size() - 1) {
            switchToTimerMode();
            resetSignalChecks();
        }
    }

    private void resetSignalChecks() {
        poorSignalStartTime = 0;
        lastLocations.clear();
    }

    private void updateUserPosition(Location location) {
        if (route == null || route.isEmpty()) return;

        Station nearestStation = findNearestStation(location.getLatitude(), location.getLongitude());
        if (nearestStation == null) return;

        int newIndex = findStationIndex(nearestStation);
        if (newIndex == -1) return;

        if (newIndex < currentStationIndex) return;

        if (nearestStation != null) {
            int nearestStationIndex = findStationIndex(nearestStation);
            long currentTime = System.currentTimeMillis();
            boolean enoughTimePassed = (currentTime - lastStationUpdateTime) >= MIN_STATION_TIME;

            if ((enoughTimePassed && nearestStationIndex - 1 == currentStationIndex) || isFirstUpdate) {
                Station previousStation = currentStation;
                currentStation = nearestStation;
                currentStationIndex = nearestStationIndex;
                lastStationUpdateTime = currentTime;

                // Проверяем, нужно ли перейти на следующую станцию
                checkAndSwitchToNextStation();

                if (currentStationIndex == route.size() - 1) {
                    sendRouteCompletionBroadcast();
                    stopSelf();
                } else {
                    updateNotification(currentStation);
                    Log.d("StationTrackingService", "Updated to nearest station by location: " + currentStation.getName());
                    sendStationUpdateBroadcast();
                }

                if (isFirstUpdate) {
                    isFirstUpdate = false;
                }
            }
        }
    }

    private boolean isDifferentLine(Station currentStation, Station nextStation) {
        String currentLine = getLineForStation(currentStation);
        String nextLine = getLineForStation(nextStation);
        return !currentLine.equals(nextLine);
    }

    private void checkAndSwitchToNextStation() {
        if (currentStation == null || route == null || route.isEmpty()) return;

        Station nextStation = findNextStation();
        if (nextStation == null) return;

        // Проверяем, находится ли следующая станция на другой линии
        if (isDifferentLine(currentStation, nextStation)) {
            // Переход на следующую станцию
            Station previousStation = currentStation;
            currentStation = nextStation;
            currentStationIndex = findStationIndex(currentStation);
            lastStationUpdateTime = System.currentTimeMillis();

            // Уведомляем пользователя о переходе
            announceTransfer(nextStation.getName(), getLineForStation(nextStation));

            // Обновляем уведомление и отправляем broadcast
            updateNotification(currentStation);
            sendStationUpdateBroadcast();

            // Проверяем, не достигли ли мы конечной станции
            if (currentStationIndex == route.size() - 1) {
                sendRouteCompletionBroadcast();
                stopSelf();
            }
        }
    }

    private void sendRouteCompletionBroadcast() {
        announceFinalArrival();
        Intent intent = new Intent("com.nicorp.nimetro.ROUTE_COMPLETED");
        intent.putExtra("finalStation", currentStation);
        sendBroadcast(intent);
    }

    private void sendStationUpdateBroadcast() {
        Intent intent = new Intent("com.nicorp.nimetro.UPDATE_STATION");
        intent.putExtra("currentStation", currentStation);
        sendBroadcast(intent);
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