package com.nicorp.nimetro.presentation.fragments;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.RECEIVER_EXPORTED;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
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
    private TextToSpeech textToSpeech;
    private TextToSpeech textToSpeechInfo;
    private AnimatedPathMapView transferMapView;

    private enum State {
        COLLAPSED,
        INFO,
        ROUTE,
        TRANSFER
    }
    private State currentState = State.INFO;
    private boolean isAnimating = false;
    private boolean isShowingTransferMap = false;

    private TextView routeTime;
    private TextView routeStationsCount;
    private TextView routeTransfersCount;
    private TextView routeTitle;
    private TextView routeTimeTitle;
    private TextView routeStationsCountTitle;
    private TextView routeTransfersCountTitle;
    private TextView nearestTrainsTitle;
    private LinearLayout routeDetailsContainer;
    private android.widget.ScrollView routeScrollView;
    private LinearLayout layoutSummary;
    private RecyclerView summaryNearestTrainsRV;
    private TextView summaryRouteTime, summaryRouteStations;
    private LinearLayout layoutInfo;
    private LinearLayout layoutRoute;
    private LinearLayout layoutTransfer;
    private FrameLayout routeInfoContainer;
    private MetroMapView metroMapView;
    private MainActivity mainActivity;
    private RecyclerView nearestTrainsRecyclerView;
    private TextView routeCost;
    private LinearLayout tripInfoGroup;
    private LinearLayout routeInfoGroup;
    private TextView startRouteButton;
    private boolean isTripStarted = false; // Флаг, указывающий, началась ли поездка
    private boolean isAlmostArrivedNotificationSent = false;
    private boolean isRouteActive = false;
    private float summaryHeightPx, infoHeightPx, routeHeightPx, transferHeightPx;

    // Marquee hints cycle for transfer
    private static final long MIN_TRANSFER_HINT_INTERVAL_MS = 5000L;
    private android.os.Handler transferHintsHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable transferHintsRunnable;
    private java.util.List<String> currentTransferHints = new java.util.ArrayList<>();
    private int currentTransferHintIndex = 0;
    private View dotTop, dotMiddle, dotBottom, dotTransfer;
    private int touchSlop;
    private Float gestureStartY = null;
    private Float gestureStartX = null;
    private List<YandexRaspResponse.Segment> summaryNearestSegments = new ArrayList<>();
    private TransferRoute currentTransferRoute;
    private Handler transferHandler;
    private Runnable transferCompleteRunnable;

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
        // Инициализируем Handler
        transferHandler = new Handler(Looper.getMainLooper());
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
        tripInfoGroup = view.findViewById(R.id.tripInfoGroup);
        routeInfoGroup = view.findViewById(R.id.routeInfoGroup);
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

        touchSlop = android.view.ViewConfiguration.get(requireContext()).getScaledTouchSlop();
        setViewColors(colorOnSurface);
        try {
            calculateAndSetRouteStatistics();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        setupCloseButton(view);
        setupStateChangeGestures();

        view.post(() -> {
            calculateHeights();
            transitionToState(State.INFO, false);
        });

        if (routeScrollView != null) {
            routeScrollView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        gestureStartY = event.getRawY();
                        gestureStartX = event.getRawX();
                        return false;
                    case MotionEvent.ACTION_MOVE: {
                        if (gestureStartY == null || gestureStartX == null) return false;
                        float dy = event.getRawY() - gestureStartY;
                        float dx = event.getRawX() - gestureStartX;
                        if (currentState == State.ROUTE && dy > touchSlop && Math.abs(dy) > Math.abs(dx) && isRouteListAtTop()) {
                            transitionToState(State.INFO, true);
                            gestureStartY = null;
                            gestureStartX = null;
                            return true;
                        }
                        if (currentState == State.ROUTE && dy < -touchSlop && Math.abs(dy) > Math.abs(dx) && isRouteListAtBottom() && currentTransferRoute != null) {
                            transitionToState(State.TRANSFER, true);
                            gestureStartY = null;
                            gestureStartX = null;
                            return true;
                        }
                        return false;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        gestureStartY = null;
                        gestureStartX = null;
                        return false;
                }
                return false;
            });
        }

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

    private void calculateHeights() {
        // Force measure pass for all layouts
        int measureSpec = View.MeasureSpec.makeMeasureSpec(routeInfoContainer.getWidth() - routeInfoContainer.getPaddingLeft() - routeInfoContainer.getPaddingRight(), View.MeasureSpec.AT_MOST);
        layoutSummary.measure(measureSpec, View.MeasureSpec.UNSPECIFIED);
        layoutInfo.measure(measureSpec, View.MeasureSpec.UNSPECIFIED);
        layoutRoute.measure(measureSpec, View.MeasureSpec.UNSPECIFIED);

        int titleAndCloseHeight = 0;
        View titleView = getView().findViewById(R.id.routeTitle);
        if (titleView != null) {
            titleAndCloseHeight = titleView.getHeight();
        }

        int padding = (int) (16 * getResources().getDisplayMetrics().density * 2);

        summaryHeightPx = layoutSummary.getMeasuredHeight() + titleAndCloseHeight + padding;
        infoHeightPx = layoutInfo.getMeasuredHeight() + titleAndCloseHeight + padding;
        routeHeightPx = layoutRoute.getMeasuredHeight() + titleAndCloseHeight + padding;
        if (layoutTransfer != null) {
            layoutTransfer.measure(measureSpec, View.MeasureSpec.UNSPECIFIED);
            transferHeightPx = layoutTransfer.getMeasuredHeight() + titleAndCloseHeight + padding;
        }

        float density = getResources().getDisplayMetrics().density;
        if (routeHeightPx > 500 * density) {
            routeHeightPx = 500 * density;
        }
    }


    private void startRouteTracking() {
        if (isRouteActive) return; // Уже активно
        if (route != null && !route.isEmpty()) {
            isRouteActive = true;
            isAlmostArrivedNotificationSent = false;
            tripInfoGroup.setVisibility(View.VISIBLE);
            routeInfoGroup.setVisibility(View.GONE);
            resetTransferTracking(); // Reset transfer tracking when starting a new route

            // Запуск сервиса
            Intent serviceIntent = new Intent(requireContext(), StationTrackingService.class);
            serviceIntent.putParcelableArrayListExtra("route", new ArrayList<>(route));
            serviceIntent.putParcelableArrayListExtra("lines", new ArrayList<>(mainActivity.getAllLines())); // Передаем линии
            serviceIntent.putParcelableArrayListExtra("transfers", new ArrayList<>(mainActivity.getAllTransfers()));
            serviceIntent.putExtra("currentStation", route.get(0)); // Передаем начальную станцию
            requireContext().startService(serviceIntent);

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
        tripInfoGroup.setVisibility(View.GONE);
        routeInfoGroup.setVisibility(View.VISIBLE);

        // Остановка сервиса
        Intent serviceIntent = new Intent(requireContext(), StationTrackingService.class);
        requireContext().stopService(serviceIntent);

        // Обновляем текст кнопки и заголовка
        startRouteButton.setText("Поехали");
        routeTitle.setText("Краткая информация");

        // Очищаем маршрут на карте
        if (metroMapView != null) {
            metroMapView.clearRoute();
            metroMapView.clearSelectedStations();
        }

        // Сбрасываем уведомления
        isAlmostArrivedNotificationSent = false;
        resetTransferTracking();
    }

    private final BroadcastReceiver stationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("currentStation")) {
                Station receivedStation = intent.getParcelableExtra("currentStation");
                if (receivedStation == null || route == null) return;

                // Ищем станцию в актуальном маршруте
                int newIndex = -1;
                for (int i = 0; i < route.size(); i++) {
                    if (route.get(i).getId().equals(receivedStation.getId())) {
                        newIndex = i;
                        break;
                    }
                }

                if (newIndex != -1) {
                    Log.d("RouteUpdate", "Valid station update: " + receivedStation.getName()
                            + " at index: " + newIndex);
                    updateRouteDisplay(newIndex);
                } else {
                    Log.w("RouteUpdate", "Received unknown station: " + receivedStation.getName());
                }
            }
            if (intent != null && intent.hasExtra("currentStation")) {
                Station currentStation = intent.getParcelableExtra("currentStation");
                Log.d("RouteInfoFragment", "Current station from broadcast: " + currentStation.getName() + " (" + currentStation.getId() + ")");
                
                // Найдем индекс станции в маршруте
                int routeIndex = -1;
                for (int i = 0; i < route.size(); i++) {
                    if (route.get(i).getId().equals(currentStation.getId())) {
                        routeIndex = i;
                        updateRouteDisplay(i);
                        break;
                    }
                }
                
                Log.d("RouteInfoFragment", "Current station index in route: " + routeIndex);
                Log.d("RouteInfoFragment", "Route size: " + route.size());
                
                // Обрабатываем переходы
                if (currentStation != null && routeIndex >= 0) {
                    handleTransfer(currentStation);
                }
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter("com.nicorp.nimetro.UPDATE_STATION");
            getContext().registerReceiver(stationUpdateReceiver, filter, RECEIVER_EXPORTED);
            getContext().registerReceiver(routeCompletionReceiver,
                    new IntentFilter("com.nicorp.nimetro.ROUTE_COMPLETED"),
                    RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (transferHintsRunnable != null) transferHintsHandler.removeCallbacks(transferHintsRunnable);
        if (getContext() != null) {
            getContext().unregisterReceiver(stationUpdateReceiver);
        }
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
        
        // Очищаем Handler и Runnable
        if (transferHandler != null) {
            if (transferCompleteRunnable != null) {
                transferHandler.removeCallbacks(transferCompleteRunnable);
                transferCompleteRunnable = null;
            }
            transferHandler = null;
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

            // Игнорировать станции не из маршрута
            if (nearestStationIndex == -1) return;

            // Пропускать обновления, если мы уже прошли эту станцию
            if (nearestStationIndex < currentStationIndex) return;

            // Проверка на финиш
            if (nearestStationIndex == route.size() - 1) {
                handleFinalStationArrival();
                return;
            }

            // Обновляем текущий индекс станции
            currentStationIndex = nearestStationIndex;

            // Обновляем предыдущую станцию
            previousStation = nearestStation;

            // Обновляем позицию пользователя на карте
            requireActivity().runOnUiThread(() -> {
                metroMapView.updateUserPosition(nearestStation);
            });

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

    private void handleFinalStationArrival() {
        isAlmostArrivedNotificationSent = true;
        stopRouteTracking();
        dismiss();
        showArrivalNotification();
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

                isShowingTransferMap = true;
                transferMapView.setVisibility(View.VISIBLE);

                // Скрываем все остальные элементы интерфейса
                LinearLayout routeInfoGroup = getView().findViewById(R.id.routeInfoGroup);
                if (routeInfoGroup != null) routeInfoGroup.setVisibility(View.GONE);
                if (tripInfoGroup != null) tripInfoGroup.setVisibility(View.GONE);
                if (layoutSummary != null) layoutSummary.setVisibility(View.GONE);
                if (layoutInfo != null) layoutInfo.setVisibility(View.GONE);
                if (layoutRoute != null) layoutRoute.setVisibility(View.GONE);
            }
        });
    }

    private void hideTransferMap() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (transferMapView != null) {
                Log.d("RouteInfoFragment", "Hiding transfer map");
                transferMapView.setVisibility(View.GONE);
                isShowingTransferMap = false;
                
                // Очищаем таймер
                if (transferHandler != null && transferCompleteRunnable != null) {
                    transferHandler.removeCallbacks(transferCompleteRunnable);
                    transferCompleteRunnable = null;
                }

                // Скрываем layout перехода
                if (layoutTransfer != null) {
                    layoutTransfer.setVisibility(View.GONE);
                }

                // НЕ восстанавливаем все элементы интерфейса здесь - это делает transitionToState
                // Только очищаем состояние перехода
                currentTransferRoute = null;
                
                // Принудительно обновляем лейаут
                if (routeInfoContainer != null) {
                    calculateHeights();
                    routeInfoContainer.requestLayout();
                    routeInfoContainer.invalidate();
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
        if (isShowingTransferMap) return;
        if (currentStationIndex < 0 || currentStationIndex >= route.size()) {
            return;
        }

        Station currentStation = route.get(currentStationIndex);
        metroMapView.updateUserPosition(currentStation);

        List<Station> remainingRoute = new ArrayList<>(route.subList(currentStationIndex, route.size()));

        metroMapView.setRoute(remainingRoute);
        updateRouteInfo(remainingRoute);

        // Обновляем информацию в tripInfoGroup
        updateTripInfoGroup(currentStationIndex);

        // После обновления содержимого пересчитаем высоты, чтобы исключить «сжатие» при возврате
        routeInfoContainer.post(this::calculateHeights);
    }

    private void updateTripInfoGroup(int currentStationIndex) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            // Текущая станция
            Station currentStation = route.get(currentStationIndex);
            TextView tripCurrentStationValue = getView().findViewById(R.id.tripCurrentStationValue);
            if (tripCurrentStationValue != null) {
                tripCurrentStationValue.setText(currentStation.getName());
            }

            // Следующая станция
            if (currentStationIndex < route.size() - 1) {
                Station nextStation = route.get(currentStationIndex + 1);
                TextView tripNextStationValue = getView().findViewById(R.id.tripNextStationValue);
                if (tripNextStationValue != null) {
                    tripNextStationValue.setText(nextStation.getName());
                }
                // Подсказка о пересадке (динамически)
                TextView transferHint = getView().findViewById(R.id.tripTransferHint);
                if (transferHint != null) {
                    Line curLine = getLineForStation(currentStation);
                    Line nextLine = getLineForStation(nextStation);
                    boolean isTransfer = (curLine != null && nextLine != null && curLine != nextLine && !curLine.getId().equals(nextLine.getId()));
                    if (isTransfer) {
                        String nextLineName = nextLine.getName() != null ? nextLine.getName() : ("Линия " + nextLine.getdisplayNumber());
                        String dir = "";
                        if (currentStationIndex + 2 < route.size()) {
                            Station afterNext = route.get(currentStationIndex + 2);
                            dir = " в сторону \"" + afterNext.getName() + "\"";
                        }
                        // Подготовим 6 вариантов советов и запустим цикл показа
                        currentTransferHints.clear();
                        currentTransferHintIndex = 0;
                        String base = "Пересадка на " + nextLineName + dir + ". ";
                        currentTransferHints.add(base + "Держитесь правее, удобнее из головного вагона.");
                        currentTransferHints.add(base + "Двери откроются справа. Готовьтесь к выходу.");
                        currentTransferHints.add(base + String.format("До конца маршрута ~%d мин.", calculateTotalTime(route.subList(currentStationIndex, route.size()))));
                        currentTransferHints.add(base + "Для быстрого выхода используйте выход №1 или №2.");
                        currentTransferHints.add(base + "Есть лифт/эскалатор на пересадке: ориентируйтесь по указателям.");
                        currentTransferHints.add(base + "Не задерживайтесь у дверей, проходите к центру платформы.");

                        transferHint.setSelected(true); // для marquee
                        transferHint.setVisibility(View.VISIBLE);
                        long hintInterval = calculateHintIntervalMs(currentStation, nextStation, currentTransferHints.size());

                        if (transferHintsRunnable != null) transferHintsHandler.removeCallbacks(transferHintsRunnable);
                        if (!currentTransferHints.isEmpty()) {
                            transferHint.setText(currentTransferHints.get(0));
                            currentTransferHintIndex = 1;
                            final long finalHintInterval = hintInterval;
                            transferHintsRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (getView() == null) {
                                        return;
                                    }
                                    if (currentTransferHints.isEmpty()) {
                                        transferHint.setVisibility(View.GONE);
                                        return;
                                    }
                                    String text = currentTransferHints.get(currentTransferHintIndex % currentTransferHints.size());
                                    transferHint.setText(text);
                                    currentTransferHintIndex++;
                                    transferHintsHandler.postDelayed(this, finalHintInterval);
                                }
                            };
                            transferHintsHandler.postDelayed(transferHintsRunnable, hintInterval);
                        }
                    } else {
                        transferHint.setVisibility(View.GONE);
                        if (transferHintsRunnable != null) transferHintsHandler.removeCallbacks(transferHintsRunnable);
                    }
                }
            } else {
                TextView tripNextStationValue = getView().findViewById(R.id.tripNextStationValue);
                if (tripNextStationValue != null) {
                    tripNextStationValue.setText("Конечная станция");
                }
                TextView transferHint = getView().findViewById(R.id.tripTransferHint);
                if (transferHint != null) transferHint.setVisibility(View.GONE);
            }

            // Время до конца маршрута
            int remainingTime = calculateTotalTime(route.subList(currentStationIndex, route.size()));
            TextView tripRemainingTimeValue = getView().findViewById(R.id.tripRemainingTimeValue);
            if (tripRemainingTimeValue != null) {
                tripRemainingTimeValue.setText(String.format("%d мин", remainingTime));
            }
        });
    }

    private long calculateHintIntervalMs(Station currentStation, Station nextStation, int hintsCount) {
        if (hintsCount <= 0) {
            return MIN_TRANSFER_HINT_INTERVAL_MS;
        }
        long segmentDuration = calculateSegmentDurationMs(currentStation, nextStation);
        long interval = segmentDuration / hintsCount;
        return Math.max(MIN_TRANSFER_HINT_INTERVAL_MS, interval);
    }

    private long calculateSegmentDurationMs(Station currentStation, Station nextStation) {
        if (currentStation == null || nextStation == null) {
            return TimeUnit.MINUTES.toMillis(1);
        }

        List<Station.Neighbor> neighbors = currentStation.getNeighbors();
        if (neighbors != null) {
            for (Station.Neighbor neighbor : neighbors) {
                Station neighborStation = neighbor.getStation();
                if (neighborStation != null && nextStation.getId().equals(neighborStation.getId())) {
                    int minutes = neighbor.getTime();
                    if (minutes > 0) {
                        return TimeUnit.MINUTES.toMillis(minutes);
                    }
                }
            }
        }

        Transfer transfer = findTransferBetweenStations(currentStation, nextStation);
        if (transfer != null && transfer.getTime() > 0) {
            return TimeUnit.MINUTES.toMillis(transfer.getTime());
        }

        return TimeUnit.MINUTES.toMillis(1);
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
        populateRouteDetails(routeDetailsContainer);
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

            int idx = 0;
            while (idx < route.size()) {
                Line curLine = getLineForStation(route.get(idx));
                int end = idx;
                while (end + 1 < route.size() && getLineForStation(route.get(end + 1)) == curLine) end++;

                int edges = Math.max(0, end - idx);
                int blockMinutes = edges * 2;

                // Блок ЛИНИИ
                View lineBlock = inflater.inflate(R.layout.item_route_line, container, false);
                TextView lineBlockTime = lineBlock.findViewById(R.id.lineBlockTime);
                View indPrimary = lineBlock.findViewById(R.id.lineBlockIndicatorPrimary);
                View indGap = lineBlock.findViewById(R.id.lineBlockIndicatorGap);
                View indSecondary = lineBlock.findViewById(R.id.lineBlockIndicatorSecondary);
                LinearLayout stationsContainer = lineBlock.findViewById(R.id.lineBlockStationsContainer);

                lineBlockTime.setText(blockMinutes > 0 ? String.valueOf(blockMinutes) : "");
                int c = Color.parseColor(route.get(idx).getColor());
                indPrimary.setBackgroundColor(c);
                if ("double".equals(curLine.getLineType())) {
                    indGap.setVisibility(View.VISIBLE);
                    indSecondary.setVisibility(View.VISIBLE);
                    indSecondary.setBackgroundColor(c);
                } else {
                    indGap.setVisibility(View.INVISIBLE);
                    indSecondary.setVisibility(View.INVISIBLE);
                }

                for (int s = idx; s <= end; s++) {
                    TextView name = new TextView(getContext());
                    name.setText(route.get(s).getName());
                    // Use themed color for text based on current theme
                    TypedValue tv = new TypedValue();
                    requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, tv, true);
                    name.setTextColor(tv.data);
                    name.setTextSize(16);
                    name.setPadding(0, (s==idx?0:4), 0, 0);
                    stationsContainer.addView(name);
                }
                container.addView(lineBlock);

                // Переход (если есть после блока)
                if (end < route.size() - 1) {
                    View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                    LinearLayout.LayoutParams transferParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    transferParams.setMargins(0, 8, 0, 8);
                    transferView.setLayoutParams(transferParams);
                    TextView transferTime = transferView.findViewById(R.id.transferTime);
                    Transfer tr = findTransferBetweenStations(route.get(end), route.get(end + 1));
                    int tMin = tr != null ? Math.max(1, tr.getTime()) : 3;
                    transferTime.setText(String.valueOf(tMin));
                    container.addView(transferView);
                }

                idx = end + 1;
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
        nearestTrainsTitle = view.findViewById(R.id.nearestTrainsTitle);
        routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        routeScrollView = view.findViewById(R.id.routeScrollView);
        layoutSummary = view.findViewById(R.id.layoutSummary);
        summaryNearestTrainsRV = view.findViewById(R.id.summaryNearestTrainsRV);
        summaryRouteTime = view.findViewById(R.id.summaryRouteTime);
        summaryRouteStations = view.findViewById(R.id.summaryRouteStations);
        layoutInfo = view.findViewById(R.id.layoutInfo);
        layoutRoute = view.findViewById(R.id.layoutRoute);
        layoutTransfer = view.findViewById(R.id.layoutTransfer);
        routeInfoContainer = view.findViewById(R.id.routeInfoContainer);
        routeCost = view.findViewById(R.id.routeCost);
        dotTop = view.findViewById(R.id.dotTop);
        dotMiddle = view.findViewById(R.id.dotMiddle);
        dotBottom = view.findViewById(R.id.dotBottom);
        dotTransfer = view.findViewById(R.id.dotTransfer);

        // По умолчанию скрываем блок "Ближайшие поезда"
        if (nearestTrainsRecyclerView != null) {
            nearestTrainsRecyclerView.setVisibility(View.GONE);
        }
        if (nearestTrainsTitle != null) {
            nearestTrainsTitle.setVisibility(View.GONE);
        }
        if (summaryNearestTrainsRV != null) {
            summaryNearestTrainsRV.setVisibility(View.GONE);
            summaryNearestTrainsRV.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void setViewColors(int colorOnSurface) {
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);
        summaryRouteTime.setTextColor(colorOnSurface);
        summaryRouteStations.setTextColor(colorOnSurface);
        if (nearestTrainsTitle != null) {
            nearestTrainsTitle.setTextColor(colorOnSurface);
        }
    }

    private void calculateAndSetRouteStatistics() throws JSONException {
        if (route != null && !route.isEmpty()) {
            int totalTime = calculateTotalTime();
            int stationsCount = route.size();
            int transfersCount = calculateTransfersCount();

            String timeText = String.format("%d мин", totalTime);
            String stationsText = String.format("%d ст.", stationsCount);
            routeTime.setText(timeText);
            routeStationsCount.setText(String.valueOf(stationsCount));
            routeTransfersCount.setText(String.format("%d", transfersCount));

            summaryRouteTime.setText(timeText);
            summaryRouteStations.setText(stationsText);

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

    public Line getLineForStation(Station station) {
        for (Line line : mainActivity.getAllLines()) {
            for (Station lineStation : line.getStations()) {
                if (lineStation.getId().equals(station.getId())) {
                    Log.d("RouteUpdate", "Found line for station " + station.getName() + " (" + station.getId() + "): " + line.getName());
                return line;
            }
        }
        }
        Log.d("RouteUpdate", "No line found for station " + station.getName() + " (" + station.getId() + ")");
        return null;
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {

            dismiss();
        });
    }

    private void setupStateChangeGestures() {
        routeInfoContainer.setOnTouchListener((v, event) -> {
            if (isAnimating) return true;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    gestureStartY = event.getRawY();
                    gestureStartX = event.getRawX();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (gestureStartY == null || gestureStartX == null) break;

                    float deltaY = event.getRawY() - gestureStartY;
                    float deltaX = event.getRawX() - gestureStartX;

                    if (Math.abs(deltaY) < touchSlop || Math.abs(deltaY) < Math.abs(deltaX)) {
                        break;
                    }

                    // Handle swipe
                    if (deltaY < 0) { // Swipe Up
                        handleSwipeUp();
                    } else { // Swipe Down
                        handleSwipeDown();
                    }
                    gestureStartY = null;
                    gestureStartX = null;
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    gestureStartY = null;
                    gestureStartX = null;
                    break;
            }
            return false;
        });
    }

    private void handleSwipeUp() {
        if (currentState == State.COLLAPSED) {
            transitionToState(State.INFO, true);
        } else if (currentState == State.INFO) {
            transitionToState(State.ROUTE, true);
        } else if (currentState == State.ROUTE) {
            if (isRouteListAtBottom() && currentTransferRoute != null) {
                // Убедимся, что transferMapView настроен, если мы вошли в ROUTE до анимации
                if (layoutTransfer != null && layoutTransfer.getVisibility() != View.VISIBLE) {
                    layoutSummary.setVisibility(View.GONE);
                    layoutInfo.setVisibility(View.GONE);
                    layoutRoute.setVisibility(View.GONE);
                    layoutTransfer.setVisibility(View.VISIBLE);
                }
                transitionToState(State.TRANSFER, true);
            }
        } else if (currentState == State.TRANSFER) {
            // Конечное состояние вверх — остаемся в TRANSFER
        }
    }

    private void handleSwipeDown() {
        if (currentState == State.TRANSFER) {
            transitionToState(State.ROUTE, true);
        } else if (currentState == State.INFO) {
            transitionToState(State.COLLAPSED, true);
        } else if (currentState == State.ROUTE && isRouteListAtTop()) {
            transitionToState(State.INFO, true);
        }
    }


    private boolean isRouteListAtTop() {
        if (routeScrollView == null) return true;
        return !routeScrollView.canScrollVertically(-1);
    }

    private boolean isRouteListAtBottom() {
        if (routeScrollView == null) return true;
        return !routeScrollView.canScrollVertically(1);
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
        // Ищем индекс по ID, а не по equals()
        int currentIndex = -1;
        for (int i = 0; i < route.size(); i++) {
            if (route.get(i).getId().equals(currentStation.getId())) {
                currentIndex = i;
                break;
            }
        }
        
        Log.d("RouteUpdate", "Current station ID: " + currentStation.getId() + " found at index: " + currentIndex);
        
        if (currentIndex == -1 || currentIndex >= route.size() - 1) {
            return null; // Текущая станция не найдена или это последняя станция
        }

        Line currentLine = getLineForStation(currentStation);
        Line nextLine = getLineForStation(route.get(currentIndex + 1));
        Log.d("RouteUpdate", "Current line: " + (currentLine != null ? currentLine.getName() : "null") + 
               ", next line: " + (nextLine != null ? nextLine.getName() : "null"));

        if (currentLine != null && nextLine != null && currentLine != nextLine) {
            Log.d("RouteUpdate", "Found transfer station: " + route.get(currentIndex + 1).getName() + " (" + route.get(currentIndex + 1).getId() + ")");
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
            // Находим индекс текущей станции по ID
            int currentIndex = -1;
            for (int i = 0; i < route.size(); i++) {
                if (route.get(i).getId().equals(currentStation.getId())) {
                    currentIndex = i;
                    break;
                }
            }
            
            // Находим индекс станции пересадки по ID
            int transferIndex = -1;
            for (int i = 0; i < route.size(); i++) {
                if (route.get(i).getId().equals(nextTransferStation.getId())) {
                    transferIndex = i;
                    break;
                }
            }
            
            Log.d("RouteInfoFragment", "Current index: " + currentIndex + ", transfer index: " + transferIndex);

            // Если текущая станция - это станция пересадки
            if (currentIndex == transferIndex - 1) {
                // Получаем предыдущую станцию из маршрута
                Log.d("RouteInfoFragment", "Current station: " + currentStation.getName());
                Station prevStation = (currentIndex > 0) ? route.get(currentIndex - 1) : null;

                Transfer transfer = findTransferBetweenStations(currentStation, nextTransferStation);
                if (transfer != null) {
                    Log.d("RouteInfoFragment", "Transfer found! Current: " + currentStation.getName() + " (" + currentStation.getId() + ")");
                    Log.d("RouteInfoFragment", "Next transfer: " + nextTransferStation.getName() + " (" + nextTransferStation.getId() + ")");
                    Log.d("RouteInfoFragment", "Prev station: " + (prevStation != null ? prevStation.getName() + " (" + prevStation.getId() + ")" : "null"));
                    
                    // Передаем предыдущую станцию в getTransferRoute
                    TransferRoute transferRoute = transfer.getTransferRoute(prevStation, currentStation, nextTransferStation);
                    if (transferRoute != null && transferRoute.getTransferMap() != null) {
                        Log.d("RouteInfoFragment", "TransferRoute found! Map: " + transferRoute.getTransferMap());
                        Log.d("RouteInfoFragment", "Route from: " + transferRoute.getFrom() + " to: " + transferRoute.getTo() + " prev: " + transferRoute.getPrev());
                        // Отображаем карту перехода БЕЗ автоматического перехода на следующую станцию
                        currentTransferRoute = transferRoute;
                        displayTransferMap(transferRoute, transfer);
                        return; // Выходим из функции, не обновляя индекс станции
                    } else {
                        Log.d("RouteInfoFragment", "TransferRoute NOT found or no transfer_map");
                        if (transferRoute == null) {
                            Log.d("RouteInfoFragment", "transferRoute is null");
                            
                            // ВРЕМЕННОЕ РЕШЕНИЕ: создаём тестовый TransferRoute для Китая-города
                            if (currentStation.getId().equals("METRO_712") && nextTransferStation.getId().equals("METRO_610")) {
                                Log.d("RouteInfoFragment", "Creating test transfer route for China Town");
                                List<String> testWay = Arrays.asList("600:400", "600:800");
                                TransferRoute testRoute = new TransferRoute("cross_2_2", "METRO_712", "METRO_610", testWay, "METRO_713", "METRO_611");
                                // Создаем тестовый Transfer с временем 3 секунды
                                Transfer testTransfer = new Transfer(Arrays.asList(currentStation, nextTransferStation), 3, "default", "cross_2_2", null);
                                displayTransferMap(testRoute, testTransfer);
                                return; // Выходим из функции, не обновляя индекс станции
                            }
                        } else if (transferRoute.getTransferMap() == null) {
                            Log.d("RouteInfoFragment", "transferRoute.getTransferMap() is null");
                        }
                    }
                } else {
                    Log.d("RouteInfoFragment", "Transfer NOT found between stations");
                }
            }
        }
    }

    private final BroadcastReceiver routeCompletionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isRouteActive) {
                if (route != null && !route.isEmpty()) {
                    updateRouteDisplay(route.size() - 1);
                }
                stopRouteTracking();
                showRouteCompletion(); // Показываем уведомление о завершении маршрута
                dismiss(); // Закрываем фрагмент
                showArrivalNotification(); // Показываем уведомление в статус-баре
            }
        }
    };

    private void showRouteCompletion() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            // Обновляем текст кнопки
            startRouteButton.setText("Маршрут завершен");
            startRouteButton.setEnabled(false); // Делаем кнопку неактивной

            // Показываем сообщение о завершении маршрута
            TextView completionMessage = new TextView(getContext());
            completionMessage.setText("Вы прибыли в конечную точку маршрута");
            completionMessage.setTextSize(18);
            // Themed text color for completion message
            TypedValue tv2 = new TypedValue();
            requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, tv2, true);
            completionMessage.setTextColor(tv2.data);
            completionMessage.setGravity(Gravity.CENTER);

            // Добавляем сообщение в layout
            routeDetailsContainer.addView(completionMessage, 0); // Добавляем в начало контейнера

            // Очищаем маршрут на карте
            if (metroMapView != null) {
                metroMapView.clearRoute();
                metroMapView.clearSelectedStations();
            }
        });
    }

    private void showArrivalNotification() {
        NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "arrival_channel")
                .setSmallIcon(R.drawable.ic_m_icon)
                .setContentTitle("Маршрут завершен")
                .setContentText("Вы прибыли в конечную точку маршрута")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (manager != null) {
            manager.notify(4, builder.build());
        }
    }

    private void displayTransferMap(TransferRoute transferRoute, Transfer transfer) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (transferMapView != null) {
                Log.d("RouteInfoFragment", "Displaying transfer map for route: " + transferRoute.getTransferMap());
                
                // Переходим в состояние TRANSFER и показываем четвёртую точку
                if (layoutTransfer != null) {
                    layoutSummary.setVisibility(View.GONE);
                    layoutInfo.setVisibility(View.GONE);
                    layoutRoute.setVisibility(View.GONE);
                    layoutTransfer.setVisibility(View.VISIBLE);
                    currentState = State.TRANSFER;
                    if (dotTransfer != null) dotTransfer.setVisibility(View.VISIBLE);
                    updateDots();
                    updateTitle();
                }

                isShowingTransferMap = true;

                // Показываем transferMapView и принудительно обновляем лейаут
                transferMapView.setVisibility(View.VISIBLE);
                transferMapView.bringToFront();
                
                // Принудительно устанавливаем размер контейнера
                if (routeInfoContainer != null) {
                    routeInfoContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    routeInfoContainer.requestLayout();
                    routeInfoContainer.invalidate();
                }

                // Ждем завершения лейаута перед установкой анимации
                transferMapView.post(() -> {
                // Показываем карту перехода
                    transferMapView.setSvgDrawable(getDrawableResource(transferRoute.getTransferMap()));

                // Устанавливаем точки для анимации пути
                    List<PointF> points = getTransferPathPoints(transferRoute.getWay());
                transferMapView.setPath(points);

                // Настраиваем внешний вид анимации
                transferMapView.setPathColor(Color.BLUE);
                transferMapView.setStrokeWidth(10f);
                transferMapView.setDashIntervals(50f, 30f);
                    transferMapView.setAnimationDuration(1000);

                    // Принудительно обновляем отображение
                    transferMapView.invalidate();
                    transferMapView.requestLayout();
                    
                    Log.d("RouteInfoFragment", "Transfer map setup completed");
                });

                // Настраиваем автоматическое завершение перехода
                if (transferHandler != null && transferCompleteRunnable != null) {
                    transferHandler.removeCallbacks(transferCompleteRunnable);
                }
                transferCompleteRunnable = () -> {
                    Log.d("RouteInfoFragment", "Transfer completed, moving to next station");
                    completeTransfer();
                };
                // Время перехода из самого объекта Transfer (в минутах, переводим в миллисекунды)
                int transferTimeMs = transfer.getTime() * 60 * 1000;
                Log.d("RouteInfoFragment", "Transfer time: " + transfer.getTime() + " seconds");
                if (transferHandler != null) {
                    transferHandler.postDelayed(transferCompleteRunnable, transferTimeMs);
                }
            }
        });
    }

    private void completeTransfer() {
        if (currentTransferRoute == null || getActivity() == null) return;
        
        Log.d("RouteInfoFragment", "Completing transfer from " + currentTransferRoute.getFrom() + " to " + currentTransferRoute.getTo());
        
        // Сохраняем информацию о переходе перед очисткой
        String targetStationId = currentTransferRoute.getTo();
        
        // Находим индекс станции назначения в маршруте
        int targetIndex = indexOfStationInRoute(targetStationId);
        if (targetIndex >= 0) {
            Log.d("RouteInfoFragment", "Found target station at index: " + targetIndex);
            currentStationIndex = targetIndex;
            
            // Используем тот же метод, что и при обычной смене станций
            updateRouteDisplay(targetIndex);
            
            Log.d("RouteInfoFragment", "Successfully moved to station: " + route.get(targetIndex).getName());
        } else {
            Log.w("RouteInfoFragment", "Target station not found in route: " + targetStationId);
        }
        
        // Скрываем карту перехода и очищаем состояние
        hideTransferMap();
        
        // Переходим к обычному отображению
        transitionToState(State.INFO, true);
    }

    private int indexOfStationInRoute(String stationId) {
        if (route == null) return -1;
        for (int i = 0; i < route.size(); i++) {
            if (route.get(i).getId().equals(stationId)) return i;
        }
        return -1;
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
            boolean hasFrom = false;
            boolean hasTo = false;
            for (Station st : transfer.getStations()) {
                if (st.getId().equals(from.getId())) {
                    hasFrom = true;
                }
                if (st.getId().equals(to.getId())) {
                    hasTo = true;
                }
                if (hasFrom && hasTo) {
                return transfer;
                }
            }
        }
        return null;
    }

    private void transitionToState(State newState, boolean animate) {
        if (currentState == newState || isAnimating) {
            return;
        }

        State oldState = currentState;
        currentState = newState;

        View oldView = getViewForState(oldState);
        View newView = getViewForState(newState);
        // Всегда пересчитываем высоты перед переходом
        calculateHeights();
        float targetHeight = getHeightForState(newState);

        if (animate) {
            animateStateChange(oldView, newView, (int) targetHeight);
        } else {
            if (newState == State.INFO || newState == State.TRANSFER) {
                routeInfoContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                routeInfoContainer.getLayoutParams().height = (int) targetHeight;
            }
            oldView.setVisibility(View.GONE);
            newView.setVisibility(View.VISIBLE);
            routeInfoContainer.requestLayout();
        }

        updateDots();
        updateTitle();
    }


    private void animateStateChange(final View oldView, final View newView, final int endHeight) {
        final View view = routeInfoContainer;
        final int startHeight = view.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
            float fraction = animation.getAnimatedFraction();
            oldView.setAlpha(1f - fraction);
            newView.setAlpha(fraction);
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
                newView.setAlpha(0f);
                newView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                oldView.setVisibility(View.GONE);
                oldView.setAlpha(1f);
                // После анимации: для INFO/TRANSFER отпускаем высоту, для COLLAPSED сохраняем фикс
                if (currentState == State.INFO || currentState == State.TRANSFER) {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.requestLayout();
                }
                isAnimating = false;
            }

            @Override public void onAnimationCancel(Animator animation) { isAnimating = false; }
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }


    private void updateDots() {
        if (dotTop == null || dotMiddle == null || dotBottom == null) return;
        
        // Получаем цвет primary из темы
        Context context = getContext();
        if (context == null) return;
        int primaryColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#1976D2"));
        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        
        // Создаем drawable для активной точки
        android.graphics.drawable.GradientDrawable selectedDot = new android.graphics.drawable.GradientDrawable();
        selectedDot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        selectedDot.setSize(dp8, dp8);
        selectedDot.setColor(primaryColor);
        
        // Обновляем точки
        if (currentState == State.COLLAPSED) {
            dotTop.setBackground(selectedDot);
        } else {
            dotTop.setBackgroundResource(R.drawable.dot_unselected);
        }
        
        if (currentState == State.INFO) {
            dotMiddle.setBackground(selectedDot);
        } else {
            dotMiddle.setBackgroundResource(R.drawable.dot_unselected);
        }
        
        if (currentState == State.ROUTE) {
            dotBottom.setBackground(selectedDot);
        } else {
            dotBottom.setBackgroundResource(R.drawable.dot_unselected);
        }
        
        if (dotTransfer != null) {
            // 4-я точка видна, пока активен переход (isShowingTransferMap) ИЛИ мы в состоянии TRANSFER
            boolean showTransferDot = isShowingTransferMap || currentState == State.TRANSFER;
            dotTransfer.setVisibility(showTransferDot ? View.VISIBLE : View.GONE);
            if (currentState == State.TRANSFER) {
                dotTransfer.setBackground(selectedDot);
            } else {
                dotTransfer.setBackgroundResource(R.drawable.dot_unselected);
            }
        }
        updateSummaryMiniCards();
    }

    private void updateSummaryMiniCards() {
        if (summaryNearestTrainsRV == null) return;
        if (currentState != State.COLLAPSED || summaryNearestSegments == null || summaryNearestSegments.isEmpty()) {
            summaryNearestTrainsRV.setVisibility(View.GONE);
            return;
        }
        summaryNearestTrainsRV.setVisibility(View.VISIBLE);
        final List<YandexRaspResponse.Segment> data = new ArrayList<>(summaryNearestSegments);
        summaryNearestTrainsRV.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            class MiniVH extends RecyclerView.ViewHolder {
                TextView num, dep, arr;
                MiniVH(@NonNull View itemView) {
                    super(itemView);
                    num = itemView.findViewById(R.id.trainNumber);
                    dep = itemView.findViewById(R.id.departureTime);
                    arr = itemView.findViewById(R.id.arrivalTime);
                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_train_info_mini, parent, false);
                return new MiniVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                MiniVH vh = (MiniVH) holder;
                YandexRaspResponse.Segment s = data.get(position);
                if (s.getThread() != null) vh.num.setText(s.getThread().getNumber());
                if (s.getDeparture() != null && s.getDeparture().length() >= 16) vh.dep.setText(s.getDeparture().substring(11, 16));
                if (s.getArrival() != null && s.getArrival().length() >= 16) vh.arr.setText(s.getArrival().substring(11, 16));
            }

            @Override
            public int getItemCount() {
                return Math.min(data.size(), 3);
            }
        });
        summaryNearestTrainsRV.requestLayout();
    }

    private void updateTitle() {
        switch (currentState) {
            case COLLAPSED:
                routeTitle.setText("Маршрут");
                break;
            case INFO:
                routeTitle.setText("Краткая информация");
                break;
            case ROUTE:
                routeTitle.setText("Информация о маршруте");
                break;
            case TRANSFER:
                routeTitle.setText("Переход");
                break;
        }
    }

    private View getViewForState(State state) {
        switch (state) {
            case COLLAPSED: return layoutSummary;
            case INFO:      return layoutInfo;
            case ROUTE:     return layoutRoute;
            case TRANSFER:  return layoutTransfer;
            default:        return null;
        }
    }

    private float getHeightForState(State state) {
        switch (state) {
            case COLLAPSED: {
                float density = getResources().getDisplayMetrics().density;
                return 100f * density; // фиксированная высота 30dp для свернутого состояния
            }
            case INFO:      return infoHeightPx;
            case ROUTE:     return routeHeightPx;
            case TRANSFER:  return transferHeightPx > 0 ? transferHeightPx : infoHeightPx;
            default:        return 0;
        }
    }

    private int calculateTotalTime() {
        int totalTime = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            for (Station station : route) {
                if (station.getId().equals(route.get(i).getId())) {
                    totalTime += station.getNeighbors().get(0).getStation().getId().equals(route.get(i + 1).getId()) ? station.getNeighbors().get(0).getTime() : 3;
                }
            }
        }

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
        // Делегируем на новую группирующую версию
        populateRouteDetails(container, route);
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
        layoutRoute.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float expandedHeight = layoutRoute.getMeasuredHeight();
        float density = getResources().getDisplayMetrics().density;
        if (expandedHeight > 500 * density) {
            expandedHeight = 500 * density;
        }
        return expandedHeight;
    }

    private void determineTrainInfoDisplay(List<Station> route) {
        List<RouteSegment> segments = splitRouteIntoSegments(route);
        boolean hasSuburban = false;
        for (RouteSegment segment : segments) {
            if (segment.getLine().getTariff() instanceof APITariff) {
                hasSuburban = true;
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
        if (!hasSuburban) {
            if (nearestTrainsRecyclerView != null) nearestTrainsRecyclerView.setVisibility(View.GONE);
            if (nearestTrainsTitle != null) nearestTrainsTitle.setVisibility(View.GONE);
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
                if (segments == null || segments.isEmpty()) {
                    nearestTrainsRecyclerView.setVisibility(View.GONE);
                    if (nearestTrainsTitle != null) nearestTrainsTitle.setVisibility(View.GONE);
                } else {
                nearestTrainsRecyclerView.setVisibility(View.VISIBLE);
                    if (nearestTrainsTitle != null) nearestTrainsTitle.setVisibility(View.VISIBLE);
                setupTrainInfoAdapter(segments, nearestTrainsRecyclerView);
                }
            }

            // Сохраняем набор для свёрнутого режима и пробуем обновить мини-карточки
            summaryNearestSegments = (segments == null) ? new ArrayList<>() : new ArrayList<>(segments);
            updateSummaryMiniCards();

            if (routeDetailsContainer != null) {
                for (int i = 0; i < routeDetailsContainer.getChildCount(); i++) {
                    View view = routeDetailsContainer.getChildAt(i);
                    String tag = (String) view.getTag();
                    if (tag != null && tag.startsWith(startStation.getName() + "|")) {
                        String[] parts = tag.split("\\|");
                        if (parts.length == 2 && "APITariff".equals(parts[1])) {
                            RecyclerView nearestTrainsRV = view.findViewById(R.id.nearestTrainsRV);
                            if (nearestTrainsRV != null) {
                                if (segments == null || segments.isEmpty()) {
                                    nearestTrainsRV.setVisibility(View.GONE);
                                    TextView title = view.findViewById(R.id.nearestTrainsTitle);
                                    if (title != null) title.setVisibility(View.GONE);
                                } else {
                                nearestTrainsRV.setVisibility(View.VISIBLE);
                                    TextView title = view.findViewById(R.id.nearestTrainsTitle);
                                    if (title != null) title.setVisibility(View.VISIBLE);
                                nearestTrainsRV.setLayoutManager(
                                        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
                                );
                                setupTrainInfoAdapter(segments, nearestTrainsRV);
                                }
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