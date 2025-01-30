package com.nicorp.nimetro.services;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.RemoteViews;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

public class StationTrackingService extends Service {

    private static final String CHANNEL_ID = "StationTrackingChannel";
    private static final String ARRIVAL_CHANNEL_ID = "arrival_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long GPS_TIMEOUT = 25000; // 45 секунд
    private static final float MAX_ACCEPTABLE_ACCURACY = 0.5f; // Максимальная допустимая погрешность (метры)
    private static final long POOR_SIGNAL_DURATION_THRESHOLD = 60000; // 1 минута плохого сигнала
    private static final float MIN_MOVEMENT_DISTANCE = 150.0f; // Минимальное движение для определения стагнации
    private static final long MIN_STATION_TIME = 90000; // 90 секунд минимальное время между станциями
    private static boolean isRunning = false;

    // В класс StationTrackingService добавить поля:
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;

    private Station currentStation;
    private Station previousStation;
    private List<Station> route;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler = new Handler();
    private Runnable locationUpdateRunnable;

    private long lastLocationTime = 0;
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
    private boolean isTtsInitialized = false;
    private Queue<String> ttsQueue = new LinkedList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (isRunning) {
            stopSelf();
            return;
        }
        initSpeechRecognizer(); // Добавить эту строку
        isRunning = true;
        createNotificationChannel();
        createArrivalNotificationChannel();
        lastLocationTime = System.currentTimeMillis();
        lastStationUpdateTime = System.currentTimeMillis();

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
                updateStationByTime();
                timerHandler.postDelayed(this, 1000);
            }
        };

        handler.post(locationUpdateRunnable);
        timerHandler.post(timerRunnable);

        // Инициализация TextToSpeech
        initTextToSpeech();
    }

    private void initSpeechRecognizer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Speech", "No RECORD_AUDIO permission");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getPackageName());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("Speech", "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("Speech", "Speech started");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d("Speech", "Speech ended");
            }

            @Override
            public void onError(int error) {
                String errorMessage;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage = "Client side error";
                        return;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMessage = "Insufficient permissions";
                        return;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMessage = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = "No recognition match";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage = "RecognitionService busy";
                        return;
                    case SpeechRecognizer.ERROR_SERVER:
                        errorMessage = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage = "No speech input";
                        break;
                    default:
                        errorMessage = "Unknown error";
                }
                Log.e("Speech", "Error: " + error + " - " + errorMessage);
                restartListening();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        processSpeechResult(match);
                    }
                }
                restartListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null) {
                    for (String part : partial) {
                        processSpeechResult(part);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        startListening();
    }

    private void processSpeechResult(String text) {
        if (text.toLowerCase().contains("станция")) {
            String[] parts = text.split("станция");
            if (parts.length > 1) {
                String afterKeyword = parts[1].trim();
                Log.d("Speech", "После 'Станция': " + afterKeyword);
            }
        }
    }

    private void startListening() {
        if (!isListening && speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
            isListening = true;
        }
    }

    private void stopListening() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private void restartListening() {
        stopListening();
        new Handler().postDelayed(this::startListening, 500);
    }


    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("ru"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Русский язык не поддерживается");
                } else {
                    Set<Voice> voices = textToSpeech.getVoices();
                    if (voices != null) {
                        for (Voice voice : voices) {
                            Log.d("TTS", "Голос: " + voice.getName());
                            if (voice.getName().equals("ru-ru-x-ruf-network")) {
                                textToSpeech.setVoice(voice);
                                Log.d("TTS", "Голос ru-ru-x-ruf-network успешно установлен");
                                isTtsInitialized = true;
                                processTtsQueue(); // Обрабатываем очередь, если есть сообщения
                                break;
                            }
                        }
                    } else {
                        Log.e("TTS", "Голоса не найдены или TextToSpeech не инициализирован");
                    }
                }
            } else {
                Log.e("TTS", "Ошибка инициализации TextToSpeech");
            }
        });
    }

    private void processTtsQueue() {
        while (!ttsQueue.isEmpty() && isTtsInitialized) {
            String text = ttsQueue.poll();
            if (text != null) {
                speak(text);
            }
        }
    }

    private void speak(String text) {
        if (isTtsInitialized && textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            ttsQueue.offer(text); // Добавляем в очередь, если TTS не готов
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
        if ((currentTime - lastLocationTime) > GPS_TIMEOUT) {
            Log.d("StationTrackingService", "GPS signal lost, switching to time-based tracking");
        }
    }

    private void updateStationByTime() {
        if (currentStation == null || route == null || route.isEmpty()) return;

        Station nextStation = findNextStation();
        if (nextStation == null) return;

        float travelTime = getTravelTime(currentStation, nextStation);
        long elapsedTime = System.currentTimeMillis() - lastStationUpdateTime;

        if (elapsedTime >= travelTime * 0.85 * 60 * 1000) {
            isAtStation(nextStation, new OnStationCheckListener() {
                @Override
                public void onStationCheck(boolean isAtStation) {
                    if (isAtStation) {
                        Log.d("StationTrackingService", "Arrived at next station ввввв: " + nextStation.getName());
                        updateToNextStation(nextStation);
                    } else if (elapsedTime >= travelTime * 1.1 * 60 * 1000) {
                        Log.d("StationTrackingService", "GPS signal lost, switching to time-based tracking");
                        updateToNextStation(nextStation);
                    }
                }
            });
        }
    }

    private void isAtStation(Station station, OnStationCheckListener listener) {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onStationCheck(false); // Если нет разрешения, возвращаем false
            return;
        }

        Log.d("StationTrackingService", "isAtStation: " + station.getName());
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double distance = station.distanceTo(location.getLatitude(), location.getLongitude());
                Log.d("StationTrackingService", "Distance to " + station.getName() + ": " + distance);
                listener.onStationCheck(distance <= MAX_ACCEPTABLE_ACCURACY);
            } else {
                listener.onStationCheck(false); // Если местоположение недоступно, возвращаем false
            }
        });
    }

    // Интерфейс для колбэка
    interface OnStationCheckListener {
        void onStationCheck(boolean isAtStation);
    }

    private void updateToNextStation(Station nextStation) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStationUpdateTime >= MIN_STATION_TIME) {
            Station previousStation = currentStation;
            currentStation = nextStation;
            currentStationIndex = findStationIndex(currentStation);
            lastStationUpdateTime = currentTime;

            checkAndAnnounceTransfers(previousStation);
            updateNotification(currentStation);
            Log.d("StationTrackingService", "Updated to next station: " + currentStation.getName());
            sendStationUpdateBroadcast();

            if (currentStationIndex == route.size() - 1) {
                sendRouteCompletionBroadcast();
                stopSelf();
            }
        }
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
        return lineName.replace("ая ","ой ").replace("ая линия", "ой линии")
                .replace("линия", "линии");
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
                    }

                    updateUserPosition(location);
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

        if (poorDuration >= POOR_SIGNAL_DURATION_THRESHOLD) {
            Log.d("StationTrackingService", "Poor GPS signal for too long, switching to time-based tracking");
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

        int nearestStationIndex = findStationIndex(nearestStation);
        if (nearestStationIndex == -1) return;

        // Если текущая станция не определена, устанавливаем её как ближайшую
        if (currentStation == null) {
            currentStation = nearestStation;
            currentStationIndex = nearestStationIndex;
            lastStationUpdateTime = System.currentTimeMillis();
            return;
        }

        // Если ближайшая станция находится перед текущей, игнорируем
        if (nearestStationIndex < currentStationIndex) return;

        // Получаем следующую станцию
        Station nextStation = findNextStation();
        if (nextStation == null) return;

        Log.d("StationTrackingService", "Nearest station: " + currentStation.getName());
        Log.d("StationTrackingService", "Next station: " + nextStation.getName());

        // Получаем время в пути между текущей и следующей станцией
        float travelTime = getTravelTime(currentStation, nextStation);
        Log.d("StationTrackingService", "Travel time: " + travelTime);

        Log.d("StationTrackingService", "lastStationUpdateTime: " + lastStationUpdateTime);
        // Вычисляем, сколько времени прошло с момента последнего обновления станции
        long elapsedTime = System.currentTimeMillis() - lastStationUpdateTime;

        // Если прошло 90% времени в пути, начинаем проверять местоположение
        if (elapsedTime >= travelTime * 0.9 * 60 * 1000) {
            double distanceToNextStation = nextStation.distanceTo(location.getLatitude(), location.getLongitude());

            // Если пользователь находится в пределах допустимой погрешности от следующей станции
            if (distanceToNextStation <= MAX_ACCEPTABLE_ACCURACY) {
                Station previousStation = currentStation;
                currentStation = nextStation;
                currentStationIndex = nearestStationIndex;
                lastStationUpdateTime = System.currentTimeMillis();

                checkAndAnnounceTransfers(previousStation);

                if (currentStationIndex == route.size() - 1) {
                    sendRouteCompletionBroadcast();
                    stopSelf();
                } else {
                    updateNotification(currentStation);
                    Log.d("StationTrackingService", "Updated to nearest station by location: " + currentStation.getName());
                    sendStationUpdateBroadcast();
                }
            }
        }
    }

    private void sendRouteCompletionBroadcast() {
//        announceFinalArrival();
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
        String trackingMode = " (по времени и GPS)";

        RemoteViews customContentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
        RemoteViews customBigContentView = new RemoteViews(getPackageName(), R.layout.custom_notification_expanded);

        customContentView.setTextViewText(R.id.title2, "Станция: " + stationName + trackingMode);
        customBigContentView.setTextViewText(R.id.title2, "Станция: " + stationName + trackingMode);

        Bitmap icon = BitmapFactory.decodeResource(getBaseContext().getResources(),
                R.drawable.ic_m_icon);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_m_icon)
                .setLargeIcon(icon)
                .setCustomContentView(customContentView)
                .setCustomBigContentView(customBigContentView)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
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