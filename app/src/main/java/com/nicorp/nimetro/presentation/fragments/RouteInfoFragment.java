package com.nicorp.nimetro.presentation.fragments;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.domain.entities.APITariff;
import com.nicorp.nimetro.domain.entities.FlatRateTariff;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.RouteSegment;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Tariff;
import com.nicorp.nimetro.domain.entities.TariffCallback;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.domain.entities.TransferRoute;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.adapters.TrainInfoAdapter;
import com.nicorp.nimetro.presentation.views.AnimatedPathMapView;
import com.nicorp.nimetro.presentation.views.MetroMapView;

import org.json.JSONException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.location.Location;
import com. google. android. gms. location. LocationRequest;
import com.nicorp.nimetro.services.StationTrackingService;

public class RouteInfoFragment extends Fragment {
    private Set<String> announcedTransfers = new HashSet<>();
    private Station lastAnnouncedStation = null;
    private static final String ARG_ROUTE = "route";
    private static final int COLLAPSED_HEIGHT = 308;
    private Station previousStation = null;

    private List<Station> route;
    private boolean isExpanded = false;
    private TextToSpeech textToSpeech;
    private TextToSpeech textToSpeechInfo;
    private AnimatedPathMapView transferMapView;

    private TextView routeTime;
    private TextView routeStationsCount;
    private TextView routeTransfersCount;
    private TextView routeTitle;
    private TextView routeTimeTitle;
    private TextView routeStationsCountTitle;
    private TextView routeTransfersCountTitle;
    private LinearLayout routeDetailsContainer;
    private LinearLayout layoutCollapsed;
    private LinearLayout layoutExpanded;
    private FrameLayout routeInfoContainer;
    private MetroMapView metroMapView;
    private MainActivity mainActivity;
    private RecyclerView nearestTrainsRecyclerView;
    private TextView routeCost;
    private Button startRouteButton;
    private boolean isAlmostArrivedNotificationSent = false;
    private boolean isRouteActive = false;

    public static RouteInfoFragment newInstance(List<Station> route, MetroMapView metroMapView, MainActivity mainActivity) {
        RouteInfoFragment fragment = new RouteInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ROUTE, new Route(route));
        fragment.metroMapView = metroMapView;
        fragment.mainActivity = mainActivity;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTextToSpeech();
        if (getArguments() != null) {
            Route routeParcelable = getArguments().getParcelable(ARG_ROUTE);
            if (routeParcelable != null) {
                route = routeParcelable.getStations();
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_route_info, container, false);
        transferMapView = view.findViewById(R.id.transferMapView);

        // Инициализация кнопки
        startRouteButton = view.findViewById(R.id.startRouteButton);
        startRouteButton.setOnClickListener(v -> {
            if (isRouteActive) {
                stopRouteTracking();
            } else {
                startRouteTracking();
            }
        });

        int colorOnSurface = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

        initializeViews(view, colorOnSurface);
        setViewColors(colorOnSurface);
        try {
            calculateAndSetRouteStatistics();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        setupCloseButton(view);
        setupSwipeGestureDetector(view);

        calculateTotalCost(route, cost -> {
            routeCost.setText(String.format("%.2f руб.", cost));
        });

        nearestTrainsRecyclerView = view.findViewById(R.id.nearestTrainsRecyclerView);
        nearestTrainsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        if (route != null && !route.isEmpty()) {
            determineTrainInfoDisplay(route);
        }

        return view;
    }

    private void startRouteTracking() {
        if (isRouteActive) return; // Уже активно
        if (route != null && !route.isEmpty()) {
            isRouteActive = true;
            isAlmostArrivedNotificationSent = false;
            resetTransferTracking(); // Reset transfer tracking when starting a new route
            startLocationTracking();

            Station startStation = route.get(0);
            mainActivity.startStationTrackingService(startStation);

            // Сбрасываем текущий индекс и предыдущую станцию
            currentStationIndex = 0;
            previousStation = null;

            // Обновляем отображение маршрута
            updateRouteDisplay(0); // Начинаем с первой станции

            // Обновляем текст кнопки и заголовка
            startRouteButton.setText("Остановить");
            routeTitle.setText("Информация о маршруте");
        }
    }

    private void stopRouteTracking() {
        if (!isRouteActive) return; // Уже остановлено
        isRouteActive = false;
        stopLocationTracking();
        mainActivity.stopStationTrackingService();

        // Обновляем текст кнопки и заголовка
        startRouteButton.setText("Поехали");
        routeTitle.setText("Краткая информация");

        // Сбрасываем текущий индекс и предыдущую станцию
        currentStationIndex = 0;
        previousStation = null;

        // Очищаем маршрут на карте
        if (metroMapView != null) {
            metroMapView.clearRoute();
            metroMapView.clearSelectedStations();
        }

        // Сбрасываем уведомления
        isAlmostArrivedNotificationSent = false;
        resetTransferTracking();
    }

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;

    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{ACCESS_FINE_LOCATION}, 1);
            return;
        }

        isTracking = true;

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isTracking) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateUserPositionOnRoute(location);
                }
            }
        };

        LocationRequest locationRequest = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(2000)
                    .build();
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationTracking() {
        if (isTracking) {
            isTracking = false;
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onDestroy() {
        if (isRouteActive) {
            stopRouteTracking();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        stopLocationTracking();
    }

    private int currentStationIndex = 0; // Добавляем переменную для хранения текущего индекса станции

    private void updateUserPositionOnRoute(Location userLocation) {
        if (route == null || route.isEmpty()) {
            return;
        }

        Station nearestStation = findNearestStation(userLocation.getLatitude(), userLocation.getLongitude());
        if (nearestStation != null) {
            int nearestStationIndex = route.indexOf(nearestStation);

            // Проверка на возврат к пройденным станциям
            if (nearestStationIndex < currentStationIndex) {
                return; // Игнорируем это обновление
            }

            // Проверка на ложное перемещение (не дальше чем на одну станцию)
            if (Math.abs(nearestStationIndex - currentStationIndex) > 1) {
                return; // Игнорируем это обновление
            }

            // Обновляем текущий индекс станции
            currentStationIndex = nearestStationIndex;

            // Обновляем предыдущую станцию
            previousStation = nearestStation;

            metroMapView.updateUserPosition(nearestStation);

            // Обновляем отображение маршрута
            updateRouteDisplay(currentStationIndex);

            // Обрабатываем переход на следующую линию, если это необходимо
            handleTransfer(nearestStation);

            // Проверяем, близко ли пользователь к конечной станции
            checkIfNearFinalStation(nearestStation);

            // Проверяем, нужно ли предупредить о переходе
            Station nextTransferStation = findNextTransferStation(route, nearestStation);
            Station TransferStation = findTransferStation(route, nearestStation);
            if (nextTransferStation != null && TransferStation == null) {
                int currentIndex = route.indexOf(nearestStation);
                int transferIndex = route.indexOf(nextTransferStation);

                // Если до перехода осталась одна станция
                Log.d("RouteUpdate", "Current index: " + currentIndex + ", transfer index: " + transferIndex);
                if (transferIndex - currentIndex == 2) {
                    Log.d("RouteUpdate", "Warning about transfer");
                    warnAboutTransfer(nearestStation, nextTransferStation);
                }
            }
        }
    }

    private void checkIfNearFinalStation(Station currentStation) {
        if (route == null || route.isEmpty() || isAlmostArrivedNotificationSent) {
            return;
        }

        // Получаем конечную станцию маршрута
        Station finalStation = route.get(route.size() - 1);

        // Если текущая станция - предпоследняя или конечная
        int currentStationIndex = route.indexOf(currentStation);
        if (currentStationIndex == route.size() - 2 || currentStationIndex == route.size() - 1) {
            sendAlmostArrivedNotification(finalStation);
            isAlmostArrivedNotificationSent = true; // Устанавливаем флаг, чтобы уведомление больше не отправлялось
        }
    }

    private void initTextToSpeech() {
        Log.d("TTS", "Инициализация TextToSpeech");
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Устанавливаем русский язык
                int result = textToSpeech.setLanguage(new Locale("ru"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Русский язык не поддерживается");
                } else {
                    Log.d("TTS", "Русский язык успешно установлен");
                }

                // Получаем список доступных голосов
                Set<Voice> voices = textToSpeech.getVoices();
                for (Voice voice : voices) {
                    Log.d("TTS", "Голос: " + voice.getName());
                    // Ищем голос с именем "ru-RU-Standard-B"
                    if (voice.getName().equals("ru-ru-x-ruf-network")) {
                        // Устанавливаем найденный голос
                        textToSpeech.setVoice(voice);
                        Log.d("TTS", "Голос ru-RU-Standard-B успешно установлен");
                        break;
                    }
                }
            } else {
                Log.e("TTS", "Ошибка инициализации TextToSpeech");
            }
        });

        textToSpeechInfo = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Устанавливаем русский язык
                int result = textToSpeechInfo.setLanguage(new Locale("ru"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Русский язык не поддерживается");
                }

                // Получаем список доступных голосов
                Set<Voice> voices = textToSpeechInfo.getVoices();
                for (Voice voice : voices) {
                    // Ищем голос с именем "ru-RU-Standard-B"
                    if (voice.getName().equals("ru-ru-x-ruf-network")) {
                        // Устанавливаем найденный голос
                        textToSpeechInfo.setVoice(voice);
                        Log.d("TTS", "Голос ru-RU-Standard-B успешно установлен");
                        break;
                    }
                }
            } else {
                Log.e("TTS", "Ошибка инициализации TextToSpeech");
            }
        });
    }

    private void sendAlmostArrivedNotification(Station finalStation) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // Создаем канал уведомлений (если не создан)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "arrival_channel",
                    "Arrival Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        String notificationText = "Следущая станция: " + finalStation.getName() + " является концом маршрута";

        // Создаем уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "arrival_channel")
                .setSmallIcon(R.drawable.ic_m_icon)
                .setContentTitle("Почти на месте!")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Показываем уведомление
        notificationManager.notify(2, builder.build());

        // Озвучиваем уведомление
        if (textToSpeech != null) {
            textToSpeech.speak(notificationText, TextToSpeech.QUEUE_FLUSH, null, "arrival_notification");
        }
    }

    private void warnAboutTransfer(Station currentStation, Station transferStation) {
        if (transferStation == null || currentStation == null) {
            return;
        }

        // Create a unique key for this transfer
        String transferKey = currentStation.getName() + "_to_" + transferStation.getName();

        // Check if we've already announced this transfer
        if (announcedTransfers.contains(transferKey)) {
            return;
        }

        // Check if we're announcing for the same station again
        if (lastAnnouncedStation != null && lastAnnouncedStation.equals(currentStation)) {
            return;
        }


        String warningMessage = "На следующей, перейдите на станцию " + transferStation.getName() +
                " " + Objects.requireNonNull(getLineForStation(transferStation)).getName().replace("линия", "линии");

        // Send notification and speak the warning
        sendTransferNotification(warningMessage);
        if (textToSpeech != null) {
            textToSpeech.speak(warningMessage, TextToSpeech.QUEUE_FLUSH, null, "transfer_warning");
        }

        // Mark this transfer as announced
        announcedTransfers.add(transferKey);
        lastAnnouncedStation = currentStation;
    }

    // Add this to onDestroy() or when stopping tracking
    private void resetTransferTracking() {
        announcedTransfers.clear();
        lastAnnouncedStation = null;
    }

    private void sendTransferNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "transfer_channel")
                .setSmallIcon(R.drawable.ic_m_icon)
                .setContentTitle("Переход на другую линию")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(3, builder.build());
    }

    private Station findNextTransferStation(List<Station> route, Station currentStation) {
        int currentIndex = route.indexOf(currentStation);
        if (currentIndex == -1 || currentIndex >= route.size() - 2) {
            return null; // Текущая станция не найдена или это последняя станция
        }

        Line currentLine = getLineForStation(currentStation);
        Line nextLine = getLineForStation(route.get(currentIndex + 2));

        if (currentLine != null && nextLine != null && currentLine != nextLine) {
            return route.get(currentIndex + 2);
        }

        return null;
    }

    private void displayTransferMap(String transferMap) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (transferMapView != null) {
                // Показываем карту перехода
                transferMapView.setSvgDrawable(R.drawable.cross_1_1); // Замените на ваш SVG

                // Устанавливаем точки для анимации пути
                List<PointF> points = getTransferPathPoints(transferMap); // Метод для получения точек пути
                transferMapView.setPath(points);

                // Настраиваем внешний вид анимации
                transferMapView.setPathColor(Color.BLUE);
                transferMapView.setStrokeWidth(10f);
                transferMapView.setDashIntervals(50f, 30f);
                transferMapView.setAnimationDuration(1000); // 1 секунда на цикл

                transferMapView.setVisibility(View.VISIBLE);

                // Скрываем группу элементов "Время", "Станций" и т.д.
                LinearLayout routeInfoGroup = getView().findViewById(R.id.routeInfoGroup);
                if (routeInfoGroup != null) {
                    routeInfoGroup.setVisibility(View.GONE);
                }
            }
        });
    }

    private void hideTransferMap() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (transferMapView != null) {
                transferMapView.setVisibility(View.GONE);

                // Показываем группу элементов "Время", "Станций" и т.д.
                LinearLayout routeInfoGroup = getView().findViewById(R.id.routeInfoGroup);
                if (routeInfoGroup != null) {
                    routeInfoGroup.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private List<PointF> getTransferPathPoints(String transferMap) {
        // Здесь реализуйте логику для получения точек пути на основе transferMap
        // Например, можно использовать предопределенные точки или загружать их из ресурсов
        return Arrays.asList(
                new PointF(790f, 1050f),
                new PointF(720f, 910f),
                new PointF(1030f, 750f),
                new PointF(960f, 610f),
                new PointF(920f, 600f),
                new PointF(930f, 570f),
                new PointF(270f, 180f)
        );
    }

    private void updateRouteDisplay(int currentStationIndex) {
        if (currentStationIndex < 0 || currentStationIndex >= route.size()) {
            return;
        }

        List<Station> remainingRoute = new ArrayList<>(route.subList(currentStationIndex, route.size()));

        metroMapView.setRoute(remainingRoute);
        updateRouteInfo(remainingRoute);
    }

    private void updateRouteInfo(List<Station> remainingRoute) {
        if (remainingRoute == null || remainingRoute.isEmpty()) {
            return;
        }

        // Пересчитываем время в пути, количество станций и пересадок
        int totalTime = calculateTotalTime(remainingRoute);
        int stationsCount = remainingRoute.size();
        int transfersCount = calculateTransfersCount(remainingRoute);

        // Обновляем текстовые поля
        routeTime.setText(String.format("%d мин", totalTime));
        routeStationsCount.setText(String.format("%d", stationsCount));
        routeTransfersCount.setText(String.format("%d", transfersCount));

        // Пересчитываем стоимость маршрута
        calculateTotalCost(remainingRoute, cost -> {
            routeCost.setText(String.format("%.2f руб.", cost));
        });

        // Обновляем список станций в интерфейсе
        populateRouteDetails(routeDetailsContainer, remainingRoute);
    }

    private int calculateTransfersCount(List<Station> route) {
        if (route == null || route.isEmpty()) {
            return 0;
        }

        int transfers = 0;
        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                transfers++; // Считаем пересадку, если цвет линии изменился
            }
        }
        return transfers;
    }

    private int calculateTotalTime(List<Station> route) {
        int totalTime = 0;
        totalTime += (route.size() - 1) * 2; // Время на проезд между станциями

        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                totalTime += 3; // Время на пересадку
            }
        }
        return totalTime;
    }

    private void populateRouteDetails(LinearLayout container, List<Station> route) {
        container.post(() -> {
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (int i = 0; i < route.size(); i++) {
                Station station = route.get(i);
                Line line = getLineForStation(station);

                View stationView = inflater.inflate(R.layout.item_route_station, container, false);
                TextView stationName = stationView.findViewById(R.id.stationName);
                View stationIndicator = stationView.findViewById(R.id.stationIndicator);
                View stationIndicatorDouble = stationView.findViewById(R.id.stationIndicatorDouble);
                RecyclerView nearestTrainsRV = stationView.findViewById(R.id.nearestTrainsRV);

                stationName.setText(station.getName());
                stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

                if (nearestTrainsRV != null) {
                    nearestTrainsRV.setVisibility(View.GONE);
                }

                if (isLineDouble(station)) {
                    stationIndicatorDouble.setVisibility(View.VISIBLE);
                    stationIndicatorDouble.setBackgroundColor(Color.parseColor(station.getColor()));
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 0);
                stationView.setLayoutParams(params);

                if (i == 0 || i == route.size() - 1) {
                    stationName.setTypeface(null, Typeface.BOLD);
                }

                String tag = station.getName() + "|" + line.getTariff().getName();
                stationView.setTag(tag);

                container.addView(stationView);

                if (i < route.size() - 1 && !route.get(i + 1).getColor().equals(station.getColor())) {
                    View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                    LinearLayout.LayoutParams transferParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    transferParams.setMargins(0, 8, 0, 8);
                    transferView.setLayoutParams(transferParams);
                    container.addView(transferView);
                }
            }

            container.requestLayout();
        });
    }

    private Station findNearestStation(double userLatitude, double userLongitude) {
        Station nearestStation = null;
        double minDistance = Double.MAX_VALUE;

        for (Station station : route) {
            double distance = station.distanceTo(userLatitude, userLongitude);
            if (distance < minDistance) {
                minDistance = distance;
                nearestStation = station;
            }
        }

        return nearestStation;
    }

    private void initializeViews(View view, int colorOnSurface) {
        routeTime = view.findViewById(R.id.routeTime);
        routeStationsCount = view.findViewById(R.id.routeStationsCount);
        routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        routeTitle = view.findViewById(R.id.routeTitle);
        routeTimeTitle = view.findViewById(R.id.routeTimeTitle);
        routeStationsCountTitle = view.findViewById(R.id.routeStationsTitle);
        routeTransfersCountTitle = view.findViewById(R.id.routeTransfersTitle);
        routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        layoutCollapsed = view.findViewById(R.id.layoutCollapsed);
        layoutExpanded = view.findViewById(R.id.layoutExpanded);
        routeInfoContainer = view.findViewById(R.id.routeInfoContainer);
        routeCost = view.findViewById(R.id.routeCost);
    }

    private void setViewColors(int colorOnSurface) {
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);
    }

    private void calculateAndSetRouteStatistics() throws JSONException {
        if (route != null && !route.isEmpty()) {
            int totalTime = calculateTotalTime();
            int stationsCount = route.size();
            int transfersCount = calculateTransfersCount();

            routeTime.setText(String.format("%d мин", totalTime));
            routeStationsCount.setText(String.format("%d", stationsCount));
            routeTransfersCount.setText(String.format("%d", transfersCount));

            populateRouteDetails(routeDetailsContainer);
        }
    }

    private void calculateTotalCost(List<Station> route, TariffCallback callback) {
        AtomicReference<Double> totalCost = new AtomicReference<>(0.0);
        List<RouteSegment> segments = splitRouteIntoSegments(route);
        boolean flatRateTariffApplied = false;

        for (RouteSegment segment : segments) {
            Tariff tariff = segment.getLine().getTariff();
            if (tariff != null) {
                if (tariff instanceof APITariff) {
                    tariff.calculateCost(segment, cost -> {
                        totalCost.updateAndGet(v -> v + cost);
                        callback.onCostCalculated(totalCost.get());
                    });
                } else if (tariff instanceof FlatRateTariff && !flatRateTariffApplied) {
                    flatRateTariffApplied = true;
                    tariff.calculateCost(segment, cost -> {
                        totalCost.updateAndGet(v -> v + cost);
                        callback.onCostCalculated(totalCost.get());
                    });
                }
            }
        }
    }

    private List<RouteSegment> splitRouteIntoSegments(List<Station> route) {
        List<RouteSegment> segments = new ArrayList<>();
        Line currentLine = null;
        List<Station> currentSegment = new ArrayList<>();

        for (Station station : route) {
            Line stationLine = getLineForStation(station);
            if (currentLine == null) {
                currentLine = stationLine;
            }

            if (stationLine != currentLine) {
                segments.add(new RouteSegment(currentSegment, currentLine, 0));
                currentSegment = new ArrayList<>();
                currentLine = stationLine;
            }

            currentSegment.add(station);
        }

        if (!currentSegment.isEmpty()) {
            segments.add(new RouteSegment(currentSegment, currentLine, 0));
        }

        return segments;
    }

    private Line getLineForStation(Station station) {
        for (Line line : mainActivity.getAllLines()) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        return null;
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {

            dismiss();
        });
    }

    private void setupSwipeGestureDetector(View view) {
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityY) > Math.abs(velocityX)) {
                    if (velocityY > 0) {
                        if (isExpanded) {
                            collapse();
                        }
                    } else {
                        if (!isExpanded) {
                            expand();
                        }
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        routeInfoContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void dismiss() {
        if (getActivity() != null) {
            if (isRouteActive) {
                stopRouteTracking();
            }

            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();

            if (metroMapView != null) {
                metroMapView.clearRoute();
                metroMapView.clearSelectedStations();
            }

            if (mainActivity != null) {
                mainActivity.clearRouteInputs();
            }

            mainActivity.stopStationTrackingService();

            isAlmostArrivedNotificationSent = false;
            resetTransferTracking(); // Clear transfer tracking when dismissing

            // Сбрасываем текущий индекс и предыдущую станцию
            currentStationIndex = 0;
            previousStation = null;

            Log.d("RouteInfoFragment", "Fragment dismissed and route cleared.");
        }
    }

    private Station findTransferStation(List<Station> route, Station currentStation) {
        int currentIndex = route.indexOf(currentStation);
        if (currentIndex == -1 || currentIndex >= route.size() - 1) {
            return null; // Текущая станция не найдена или это последняя станция
        }

        Line currentLine = getLineForStation(currentStation);
        Line nextLine = getLineForStation(route.get(currentIndex + 1));
        Log.d("RouteUpdate", "Current line: " + currentLine + ", next line: " + nextLine);

        if (currentLine != null && nextLine != null && currentLine != nextLine) {
            Log.d("RouteUpdate", "Found transfer station: " + route.get(currentIndex + 1));
            return route.get(currentIndex + 1);
        }

        return null;
    }

    private void handleTransfer(Station currentStation) {
        if (currentStation == null || route == null || route.isEmpty()) {
            return;
        }

        // Находим следующую станцию пересадки
        Station nextTransferStation = findTransferStation(route, currentStation);
        if (nextTransferStation != null) {
            // Находим индекс текущей станции
            int currentIndex = route.indexOf(currentStation);
            int transferIndex = route.indexOf(nextTransferStation);

            // Если текущая станция - это станция пересадки
            if (currentIndex == transferIndex - 1) {
                // Получаем предыдущую станцию из маршрута
                Log.d("RouteInfoFragment", "Current station: " + currentStation.getName());
                Station prevStation = (currentIndex > 0) ? route.get(currentIndex - 1) : null;
//                Log.d("RouteInfoFragment", "Previous station: " + Objects.requireNonNull(prevStation).getName());

                // Обновляем текущую станцию на следующую (первую станцию новой линии)
                currentStationIndex = transferIndex;
                previousStation = nextTransferStation;

                Transfer transfer = findTransferBetweenStations(currentStation, nextTransferStation);
                if (transfer != null) {
                    // Передаем предыдущую станцию в getTransferRoute
                    TransferRoute transferRoute = transfer.getTransferRoute(prevStation, currentStation, nextTransferStation);
                    if (transferRoute != null && transferRoute.getTransferMap() != null) {
                        Log.d("RouteInfoFragment", "Transfer found between " + transfer.getStations().get(0));
                        // Отображаем карту перехода
                        displayTransferMap(transferRoute);
                    }
                }

                // Обновляем отображение маршрута
                updateRouteDisplay(currentStationIndex);

                // Оповещаем пользователя о переходе
                String transferMessage = "Перейдите на станцию " + nextTransferStation.getName() + " " + Objects.requireNonNull(getLineForStation(nextTransferStation)).getName().replace("линия", "линии");
                if (textToSpeech != null) {
                    textToSpeech.speak(transferMessage, TextToSpeech.QUEUE_FLUSH, null, "transfer_notification");
                }

                // Ожидание пока текст перехода говорится
                if (textToSpeech != null) {
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            // Текст перехода начал говориться
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            // Получаем текст перехода
                            Transfer transfer = findTransferBetweenStations(currentStation, nextTransferStation);
                            String transferMessage = transfer != null
                                    ? transfer.getTransitionText(currentStation, nextTransferStation)
                                    : "Переход недоступен.";

                            // Оповещаем пользователя о переходе
                            if (textToSpeechInfo != null) {
                                textToSpeechInfo.speak(transferMessage, TextToSpeech.QUEUE_FLUSH, null, "transfer_notification");
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            // Ошибка при говорении текста перехода
                        }
                    });
                }

                // Отправляем уведомление о переходе
                sendTransferNotification(transferMessage);

                // Обновляем позицию пользователя на карте
                if (metroMapView != null) {
                    metroMapView.updateUserPosition(nextTransferStation);
                }
            }
        }
    }

    private void displayTransferMap(TransferRoute transferRoute) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (transferMapView != null) {
                // Показываем карту перехода
                transferMapView.setSvgDrawable(getDrawableResource(transferRoute.getTransferMap())); // Замените на ваш метод получения ресурса

                // Устанавливаем точки для анимации пути
                List<PointF> points = getTransferPathPoints(transferRoute.getWay()); // Метод для получения точек пути
                transferMapView.setPath(points);

                // Настраиваем внешний вид анимации
                transferMapView.setPathColor(Color.BLUE);
                transferMapView.setStrokeWidth(10f);
                transferMapView.setDashIntervals(50f, 30f);
                transferMapView.setAnimationDuration(1000); // 1 секунда на цикл

                transferMapView.setVisibility(View.VISIBLE);

                // Скрываем группу элементов "Время", "Станций" и т.д.
                LinearLayout routeInfoGroup = getView().findViewById(R.id.routeInfoGroup);
                if (routeInfoGroup != null) {
                    routeInfoGroup.setVisibility(View.GONE);
                }
            }
        });
    }

    private int getDrawableResource(String transferMap) {
        // Здесь реализуйте логику для получения ресурса drawable по имени
        switch (transferMap) {
            case "cross_2_2":
                return R.drawable.cross_2_2;
            // Добавьте другие случаи по мере необходимости
            default:
                return R.drawable.cross_1_1;
        }
    }

    private List<PointF> getTransferPathPoints(List<String> way) {
        List<PointF> points = new ArrayList<>();
        for (String point : way) {
            String[] coordinates = point.split(":");
            float x = Float.parseFloat(coordinates[0]);
            float y = Float.parseFloat(coordinates[1]);
            points.add(new PointF(x, y));
        }
        return points;
    }

    private Transfer findTransferBetweenStations(Station from, Station to) {
        for (Transfer transfer : mainActivity.getAllTransfers()) {
            if (transfer.getStations().contains(from) && transfer.getStations().contains(to)) {
                return transfer;
            }
        }
        return null;
    }

    private void expand() {
        isExpanded = true;
        routeTitle.setText("Информация о маршруте");
        layoutCollapsed.setVisibility(View.GONE);
        layoutExpanded.setVisibility(View.VISIBLE);

        float expandedHeight = getExpandedHeight();
        animateHeightChange(routeInfoContainer, COLLAPSED_HEIGHT, expandedHeight);
    }

    private void collapse() {
        isExpanded = false;
        routeTitle.setText("Краткая информация");
        layoutCollapsed.setVisibility(View.VISIBLE);
        layoutExpanded.setVisibility(View.GONE);

        float density = getResources().getDisplayMetrics().density;
        animateHeightChange(routeInfoContainer, routeInfoContainer.getHeight(), COLLAPSED_HEIGHT * density);
    }

    private void animateHeightChange(final View view, final int startHeight, final float endHeight) {
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int newHeight = (int) (startHeight + (endHeight - startHeight) * interpolatedTime);
                view.getLayoutParams().height = newHeight;
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(300);
        view.startAnimation(animation);
    }

    private int calculateTotalTime() {
        int totalTime = 0;
        totalTime += (route.size() - 1) * 2;

        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                totalTime += 3;
            }
        }
        return totalTime;
    }

    private int calculateTransfersCount() {
        int transfers = 0;
        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                transfers++;
            }
        }
        return transfers;
    }

    private void populateRouteDetails(LinearLayout container) {
        container.post(() -> {
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (int i = 0; i < route.size(); i++) {
                Station station = route.get(i);
                Line line = getLineForStation(station);

                View stationView = inflater.inflate(R.layout.item_route_station, container, false);
                TextView stationName = stationView.findViewById(R.id.stationName);
                View stationIndicator = stationView.findViewById(R.id.stationIndicator);
                View stationIndicatorDouble = stationView.findViewById(R.id.stationIndicatorDouble);
                RecyclerView nearestTrainsRV = stationView.findViewById(R.id.nearestTrainsRV);

                stationName.setText(station.getName());
                stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

                if (nearestTrainsRV != null) {
                    nearestTrainsRV.setVisibility(View.GONE);
                }

                if (isLineDouble(station)) {
                    stationIndicatorDouble.setVisibility(View.VISIBLE);
                    stationIndicatorDouble.setBackgroundColor(Color.parseColor(station.getColor()));
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 0);
                stationView.setLayoutParams(params);

                if (i == 0 || i == route.size() - 1) {
                    stationName.setTypeface(null, Typeface.BOLD);
                }

                String tag = station.getName() + "|" + line.getTariff().getName();
                stationView.setTag(tag);

                Log.d("RouteInfoFragmentT", "Station: " + tag.split("\\|")[0] + ", Line: " + tag.split("\\|")[1]);

                container.addView(stationView);

                if (i < route.size() - 1 && !route.get(i + 1).getColor().equals(station.getColor())) {
                    View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                    LinearLayout.LayoutParams transferParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    transferParams.setMargins(0, 8, 0, 8);
                    transferView.setLayoutParams(transferParams);
                    container.addView(transferView);
                }
            }

            container.requestLayout();
        });
    }

    private boolean isLineDouble(Station station) {
        for (Line line : mainActivity.getAllLines()) {
            if (line.getStations().contains(station) && "double".equals(line.getLineType())) {
                return true;
            }
        }
        return false;
    }

    private float getExpandedHeight() {
        layoutExpanded.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float expandedHeight = layoutExpanded.getMeasuredHeight();
        float density = getResources().getDisplayMetrics().density;
        if (expandedHeight > 500 * density) {
            expandedHeight = 500 * density;
        }
        return expandedHeight;
    }

    private void determineTrainInfoDisplay(List<Station> route) {
        List<RouteSegment> segments = splitRouteIntoSegments(route);
        for (RouteSegment segment : segments) {
            if (segment.getLine().getTariff() instanceof APITariff) {
                if (segment.getStations().get(0).equals(route.get(0))) {
                    fetchAndDisplaySuburbanSchedule(segment.getStations().get(0),
                            segment.getStations().get(segment.getStations().size() - 1),
                            true);
                } else {
                    fetchAndDisplaySuburbanSchedule(segment.getStations().get(0),
                            segment.getStations().get(segment.getStations().size() - 1),
                            false);
                }
            }
        }
    }

    private void fetchAndDisplaySuburbanSchedule(Station startStation, Station endStation, boolean isFirstStation) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        String from = startStation.getESP();
        String to = endStation.getESP();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, from, to, "esr", date, 1000);

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                    if (!segments.isEmpty()) {
                        List<YandexRaspResponse.Segment> nearestSegments = findNearestDepartureTimes(segments);
                        displayNearestTrains(startStation, nearestSegments, isFirstStation);
                    } else {
                        Log.d("RouteInfoFragment", "No segments found for suburban schedule");
                    }
                } else {
                    Log.e("RouteInfoFragment", "Failed to fetch suburban schedule: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.e("RouteInfoFragment", "Error fetching suburban schedule", t);
            }
        });
    }

    private void displayNearestTrains(Station startStation, List<YandexRaspResponse.Segment> segments, boolean isFirstStation) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (isFirstStation && nearestTrainsRecyclerView != null) {
                nearestTrainsRecyclerView.setVisibility(View.VISIBLE);
                setupTrainInfoAdapter(segments, nearestTrainsRecyclerView);
            }

            if (routeDetailsContainer != null) {
                for (int i = 0; i < routeDetailsContainer.getChildCount(); i++) {
                    View view = routeDetailsContainer.getChildAt(i);
                    String tag = (String) view.getTag();
                    if (tag != null && tag.startsWith(startStation.getName() + "|")) {
                        String[] parts = tag.split("\\|");
                        if (parts.length == 2 && "APITariff".equals(parts[1])) {
                            RecyclerView nearestTrainsRV = view.findViewById(R.id.nearestTrainsRV);
                            if (nearestTrainsRV != null) {
                                nearestTrainsRV.setVisibility(View.VISIBLE);
                                nearestTrainsRV.setLayoutManager(
                                        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
                                );
                                setupTrainInfoAdapter(segments, nearestTrainsRV);
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    private List<YandexRaspResponse.Segment> findNearestDepartureTimes(List<YandexRaspResponse.Segment> segments) {
        List<YandexRaspResponse.Segment> nearestDepartureTimes = new ArrayList<>();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        for (YandexRaspResponse.Segment segment : segments) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                ZonedDateTime departureDateTime = ZonedDateTime.parse(segment.getDeparture(), formatter);
                long difference = ChronoUnit.MINUTES.between(currentTime, departureDateTime);

                if (difference > 0 && nearestDepartureTimes.size() < 3) {
                    nearestDepartureTimes.add(segment);
                }
            } catch (Exception e) {
                Log.e("RouteInfoFragment", "Error parsing date", e);
            }
        }

        return nearestDepartureTimes;
    }

    private void setupTrainInfoAdapter(List<YandexRaspResponse.Segment> segments, RecyclerView recyclerView) {
        TrainInfoAdapter adapter = new TrainInfoAdapter(segments, () -> {
            FullScheduleDialogFragment dialogFragment = FullScheduleDialogFragment.newInstance(segments, route.get(0).getESP(), route.get(route.size() - 1).getESP());
            dialogFragment.show(getChildFragmentManager(), "FullScheduleDialogFragment");
        });
        recyclerView.setAdapter(adapter);
        recyclerView.requestLayout();
        recyclerView.invalidate();
    }
}