package com.nicorp.nimetro.presentation.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.APITariff;
import com.nicorp.nimetro.domain.entities.FlatRateTariff;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.RouteStation;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Tariff;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.domain.entities.TransferRoute;
import com.nicorp.nimetro.domain.entities.ZoneBasedTariff;
import com.nicorp.nimetro.presentation.adapters.RoutePagerAdapter;
import com.nicorp.nimetro.presentation.adapters.StationPagerAdapter;
import com.nicorp.nimetro.presentation.views.MetroMapView;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.presentation.fragments.RouteInfoFragment;
import com.nicorp.nimetro.presentation.fragments.StationInfoFragment;
import com.nicorp.nimetro.presentation.adapters.StationsAdapter;
import com.nicorp.nimetro.domain.entities.Facilities;
import com.nicorp.nimetro.services.StationTrackingService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends AppCompatActivity implements MetroMapView.OnStationClickListener, StationInfoFragment.OnStationInfoListener, StationsAdapter.OnStationClickListener {

    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private List<Station> suburbanStations;
    private List<Line> suburbanLines;
    private Station selectedStartStation;
    private Station selectedEndStation;
    private List<Station> selectedStations;
    private List<Line> riverTramLines; // Список линий речного трамвая
    private List<Station> riverTramStations; // Список станций речного трамвая
    private List<Transfer> riverTramTransfers; // Список переходов речного трамвая
    private List<River> riverTramRivers; // Список рек для речного трамвая
    private List<MapObject> riverTramMapObjects; // Список объектов на карте речного трамвая
    private List<Line> tramLines;
    private List<Station> tramStations;
    private List<Transfer> tramTransfers;
    private List<River> tramRivers;
    private List<MapObject> tramMapObjects;

    private TextInputLayout startStationLayout;
    private TextInputLayout endStationLayout;
    private TextInputEditText startStationEditText;
    private TextInputEditText endStationEditText;
    private RecyclerView stationsRecyclerView;
    private StationsAdapter stationsAdapter;
    private ViewPager2.OnPageChangeCallback stationPagerChangeCallback;
    private StationPagerAdapter currentPagerAdapter;
    private ViewPager2.OnPageChangeCallback routePagerChangeCallback;
    private RoutePagerAdapter currentRoutePagerAdapter;

    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;
    private static final long LOCATION_UPDATE_INTERVAL = 30000; // 30 секунд

    public static boolean isMetroMap = true; // Флаг для определения текущей карты
    public static boolean isSuburbanMap = false;
    public static boolean isRiverTramMap = false;
    public static boolean isTramMap = false;


    private boolean isTrackingServiceRunning = false;

    public void startStationTrackingService(Station station) {
        if (isTrackingServiceRunning) return; // Уже запущен

        Intent serviceIntent = new Intent(this, StationTrackingService.class);
        serviceIntent.putExtra("currentStation", station);
        ArrayList<Station> routePayload = new ArrayList<>();
        if (selectedStations != null && !selectedStations.isEmpty()) {
            routePayload.addAll(selectedStations);
        } else if (station != null) {
            routePayload.add(station);
        }
        serviceIntent.putParcelableArrayListExtra("route", routePayload);
        List<Line> linesPayload = getAllLines();
        if (linesPayload == null) {
            linesPayload = Collections.emptyList();
        }
        serviceIntent.putParcelableArrayListExtra("lines", new ArrayList<>(linesPayload));
        List<Transfer> transfersPayload = getAllTransfers();
        if (transfersPayload == null) {
            transfersPayload = Collections.emptyList();
        }
        serviceIntent.putParcelableArrayListExtra("transfers", new ArrayList<>(transfersPayload));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isTrackingServiceRunning = true;
    }

    private static class MapTypeRow {
        String key; String title; int icon;
        MapTypeRow(String key, String title, int icon){ this.key=key; this.title=title; this.icon=icon; }
    }

    private void showMapTypePopup(View anchor, ImageView switchMapButton) {
        List<MapTypeRow> rows = new ArrayList<>();
        addIfExists(rows, "metro", "Метро", "metro_map_icon");
        addIfExists(rows, "suburban", "Пригород", "suburban_map_icon");
        addIfExists(rows, "river", "Речной транспорт", "river_map_icon");
        addIfExists(rows, "river_tram", "Речной трамвай", "river_tram_icon");
        addIfExists(rows, "tram", "Трамвай", "tram_map_icon");
        addIfExists(rows, "monorail", "Монорельс", "monorail_map_icon");
        addIfExists(rows, "funicular", "Фуникулёр", "funicular_map_icon");
        addIfExists(rows, "gondola", "Канатная дорога", "gondola_map_icon", "cable_car_map_icon");

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.popup_map_type, null, false);
        bindRow(content, R.id.row_metro, R.id.icon_metro, R.id.title_metro, rows, "metro");
        bindRow(content, R.id.row_suburban, R.id.icon_suburban, R.id.title_suburban, rows, "suburban");
        bindRow(content, R.id.row_river, R.id.icon_river, R.id.title_river, rows, "river");
        bindRow(content, R.id.row_river_tram, R.id.icon_river_tram, R.id.title_river_tram, rows, "river_tram");
        bindRow(content, R.id.row_tram, R.id.icon_tram, R.id.title_tram, rows, "tram");
        bindRow(content, R.id.row_monorail, R.id.icon_monorail, R.id.title_monorail, rows, "monorail");
        bindRow(content, R.id.row_funicular, R.id.icon_funicular, R.id.title_funicular, rows, "funicular");
        bindRow(content, R.id.row_gondola, R.id.icon_gondola, R.id.title_gondola, rows, "gondola");

        android.widget.PopupWindow pw = new android.widget.PopupWindow(content, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setElevation(12f);
        pw.setBackgroundDrawable(getDrawable(R.drawable.bg_popup_rounded));
        View.OnClickListener onClick = v1 -> {
            String key = (String) v1.getTag();
            switch (key) {
                case "metro":
                    isMetroMap = true;
                    isSuburbanMap = false;
                    isRiverTramMap = false;
                    isTramMap = false;
                    break;
                case "suburban":
                    isMetroMap = false;
                    isSuburbanMap = true;
                    isRiverTramMap = false;
                    isTramMap = false;
                    break;
                case "river":
                    isMetroMap = false;
                    isSuburbanMap = false;
                    isRiverTramMap = true;
                    isTramMap = false;
                    break;
                case "river_tram":
                    isMetroMap = false;
                    isSuburbanMap = false;
                    isRiverTramMap = true;
                    isTramMap = false;
                    break;
                case "tram":
                    isMetroMap = false;
                    isSuburbanMap = false;
                    isRiverTramMap = false;
                    isTramMap = true;
                    break;
                case "monorail":
                case "funicular":
                case "gondola":
                    isMetroMap = true;
                    isSuburbanMap = false;
                    isRiverTramMap = false;
                    isTramMap = false;
                    break;
            }
            // установить иконку выбранного
            for (MapTypeRow r : rows) if (r.key.equals(key)) { switchMapButton.setImageResource(r.icon); break; }
            updateMapData();
            pw.dismiss();
        };
        attachClick(content, R.id.row_metro, onClick);
        attachClick(content, R.id.row_suburban, onClick);
        attachClick(content, R.id.row_river, onClick);
        attachClick(content, R.id.row_river_tram, onClick);
        attachClick(content, R.id.row_tram, onClick);
        attachClick(content, R.id.row_monorail, onClick);
        attachClick(content, R.id.row_funicular, onClick);
        attachClick(content, R.id.row_gondola, onClick);
        // Выравниваем по правому краю кнопки: ждём измерения контента
        content.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupW = content.getMeasuredWidth();
        int offsetX = anchor.getWidth() - popupW; // прижимаем к правому краю кнопки
        int offsetY = dp(8); // отступ ВНИЗ от нижнего края кнопки
        pw.showAsDropDown(anchor, offsetX, offsetY);
    }

    private void bindRow(View root, int rowId, int iconId, int titleId, List<MapTypeRow> rows, String key) {
        View row = root.findViewById(rowId);
        MapTypeRow r = null;
        for (MapTypeRow it : rows) if (it.key.equals(key)) { r = it; break; }
        if (r == null) { row.setVisibility(View.GONE); return; }
        ImageView iv = row.findViewById(iconId);
        TextView tv = row.findViewById(titleId);
        iv.setImageResource(r.icon);
        tv.setText(r.title);
        row.setTag(r.key);
    }

    private void attachClick(View root, int rowId, View.OnClickListener l) {
        View row = root.findViewById(rowId);
        if (row.getVisibility() == View.VISIBLE) row.setOnClickListener(l);
    }

    private void addIfExists(List<MapTypeRow> out, String key, String title, String... iconNames) {
        for (String name : iconNames) {
            int id = getResources().getIdentifier(name, "drawable", getPackageName());
            if (id != 0) { out.add(new MapTypeRow(key, title, id)); return; }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    public void stopStationTrackingService() {
        if (!isTrackingServiceRunning) return; // Уже остановлен

        Intent serviceIntent = new Intent(this, StationTrackingService.class);
        stopService(serviceIntent);
        isTrackingServiceRunning = false;
    }

    /**
     * Применяет тему из SharedPreferences перед созданием Activity.
     */
    private void applyTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");
        
        if (selectedTheme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем тему ДО super.onCreate() чтобы она применилась сразу
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocation();

        // Инициализация Handler и Runnable для периодического обновления местоположения
        locationUpdateHandler = new Handler(Looper.getMainLooper());
        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                requestLocation();
                if (locationUpdateHandler != null) {
                    locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
                }
            }
        };

        // startLocationUpdates(); // Закомментировано, так как не используется

        // Инициализация всех списков
        rivers = new ArrayList<>();
        stations = new ArrayList<>();
        lines = new ArrayList<>();
        suburbanStations = new ArrayList<>();
        suburbanLines = new ArrayList<>();
        selectedStations = new ArrayList<>();
        riverTramLines = new ArrayList<>();
        riverTramStations = new ArrayList<>();
        riverTramTransfers = new ArrayList<>();
        riverTramRivers = new ArrayList<>();
        riverTramMapObjects = new ArrayList<>();
        tramLines = new ArrayList<>();
        tramStations = new ArrayList<>();
        tramTransfers = new ArrayList<>();
        tramRivers = new ArrayList<>();
        tramMapObjects = new ArrayList<>();
        transfers = new ArrayList<>();
        mapObjects = new ArrayList<>();
        suburbanTransfers = new ArrayList<>();
        suburbanRivers = new ArrayList<>();
        suburbanMapObjects = new ArrayList<>();
        allLines = new ArrayList<>();

        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        ImageView switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> showMapTypePopup(v, switchMapButton));

        // Левая кнопка (главный экран) - уже активна, но добавляем для единообразия
        ConstraintLayout mainTabButton = findViewById(R.id.mainTabButton);
        // Не добавляем клик, так как мы уже на главном экране

        ConstraintLayout settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Settings button clicked");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        metroMapView = findViewById(R.id.metroMapView);
        startStationEditText = findViewById(R.id.startStationEditText);
        endStationEditText = findViewById(R.id.endStationEditText);
        stationsRecyclerView = findViewById(R.id.stationsRecyclerView);
        rivers = new ArrayList<>();
        stations = new ArrayList<>();
        lines = new ArrayList<>();
        suburbanStations = new ArrayList<>();
        suburbanLines = new ArrayList<>();
        selectedStations = new ArrayList<>();
        metroMapView.setOnStationClickListener(this);
        
        // Обновляем цвета карты после того, как View создан и прикреплен к окну
        metroMapView.post(() -> {
            if (metroMapView != null) {
                metroMapView.updateThemeColors();
                // Вызываем invalidate для перерисовки с новыми цветами
                metroMapView.invalidate();
            }
        });

        loadMetroData(selectedMapFileName);

        stationsAdapter = new StationsAdapter(new ArrayList<Station>(), this);
        stationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stationsRecyclerView.setAdapter(stationsAdapter);
        
        // Скрываем список станций по умолчанию
        hideStationsList();

        startStationEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showStationsList(stations);
            } else {
                hideStationsList();
            }
        });

        endStationEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showStationsList(stations);
            } else {
                hideStationsList();
            }
        });

        startStationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        endStationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setStationInfo(TextInputEditText editText, Station station) {
        if (station == null) {
            editText.setText("");
            return;
        }

        Line line = findLineByStation(station);

        SpannableString spannableString;
        if (line != null) {
            // Используем getLineDisplayNumberForStation для правильного получения displayNumber
            String displayNumber = line.getLineDisplayNumberForStation(station);
            if (displayNumber == null || displayNumber.isEmpty()) {
                // Fallback на общий displayNumber если для станции не найден
                displayNumber = line.getdisplayNumber();
            }
            if (displayNumber == null || displayNumber.isEmpty()) {
                // Если displayNumber все еще пустой, используем только название станции
                spannableString = new SpannableString(station.getName());
            } else {
                spannableString = new SpannableString(displayNumber + " " + station.getName());
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor(line.getColor()));
                spannableString.setSpan(colorSpan, 0, displayNumber.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            spannableString = new SpannableString(station.getName());
        }

        editText.setText(spannableString);
    }

    private Line findLineByStation(Station station) {
        if (station == null || allLines == null) return null;
        // Сравниваем станции по id, так как объекты могут быть разными экземплярами
        for (Line line : allLines) {
            if (line != null && line.getStations() != null) {
                for (Station s : line.getStations()) {
                    if (s != null && s.getId() != null && s.getId().equals(station.getId())) {
                        return line;
                    }
                }
            }
        }
        return null;
    }
    private ObjectAnimator startStationAnimator;
    private ObjectAnimator endStationAnimator;
    private void showStationInfoLayout(Station station, boolean isStartStation) {
        // Находим соответствующий FrameLayout
        FrameLayout container = isStartStation ? findViewById(R.id.startStationContainer) : findViewById(R.id.endStationContainer);

        // Очищаем контейнер перед добавлением нового элемента
        container.removeAllViews();

        // Создаем View из station_info_layout
        View stationInfoView = getLayoutInflater().inflate(R.layout.station_info_layout, container, false);

        // Находим TextView для номера линии и названия станции
        TextView lineNumberTextView = stationInfoView.findViewById(R.id.lineNumberTextView);
        TextView stationNameTextView = stationInfoView.findViewById(R.id.stationNameTextView);
        lineNumberTextView.setTextColor(Color.WHITE);
        stationNameTextView.setTextColor(Color.WHITE);
        ConstraintLayout containerLayout = stationInfoView.findViewById(R.id.containerLayout);

        ImageView closeButton = stationInfoView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            if (isStartStation && startStationAnimator != null) {
                startStationAnimator.cancel();
                startStationAnimator = null;
            } else if (!isStartStation && endStationAnimator != null) {
                endStationAnimator.cancel();
                endStationAnimator = null;
            }

            container.removeAllViews();
            if (isStartStation) {
                startStationEditText.setVisibility(View.VISIBLE);
                startStationEditText.setSelected(false);
                startStationEditText.setEnabled(true);
                startStationEditText.setText("");
                startStationEditText.setHint(getString(R.string.from_hint));
                if (selectedStartStation != null) {
                    selectedStations.remove(selectedStartStation);
                }
                selectedStartStation = null;
                if (startStationEditText.getParent() != container) {
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    startStationEditText.setLayoutParams(lp);
                    container.addView(startStationEditText);
                }
            } else {
                endStationEditText.setVisibility(View.VISIBLE);
                endStationEditText.setSelected(false);
                endStationEditText.setEnabled(true);
                endStationEditText.setText("");
                endStationEditText.setHint(getString(R.string.to_hint));
                if (selectedEndStation != null) {
                    selectedStations.remove(selectedEndStation);
                }
                selectedEndStation = null;
                if (endStationEditText.getParent() != container) {
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    endStationEditText.setLayoutParams(lp);
                    container.addView(endStationEditText);
                }
            }
            metroMapView.setSelectedStations(selectedStations);
            clearRoute();
            resetStationPagerIfNoSelection();
        });

        // Устанавливаем данные станции
        Line line = findLineByStation(station);
        if (line != null) {
            // Используем getLineDisplayNumberForStation для правильного получения displayNumber
            String displayNumber = line.getLineDisplayNumberForStation(station);
            Log.d("MainActivity", "Station: " + station.getName() + ", Line displayNumber from method: " + displayNumber + ", Line id: " + line.getId());
            Log.d("MainActivity", "Line general displayNumber: " + line.getdisplayNumber());
            
            // Если displayNumber не найден для станции, используем общий displayNumber линии
            if (displayNumber == null || displayNumber.isEmpty()) {
                displayNumber = line.getdisplayNumber();
                Log.d("MainActivity", "Using general displayNumber: " + displayNumber);
            }
            
            // Если displayNumber все еще null или пустой, используем id
            if (displayNumber == null || displayNumber.isEmpty()) {
                displayNumber = line.getId();
                Log.d("MainActivity", "Using line id as fallback: " + displayNumber);
            }
            
            // Устанавливаем значение в TextView
            Log.d("MainActivity", "Final displayNumber to set: " + displayNumber);
            lineNumberTextView.setText(displayNumber != null ? displayNumber : "");
            containerLayout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(line.getColor())));
        } else {
            Log.d("MainActivity", "Line not found for station: " + station.getName());
            // Устанавливаем пустую строку если линия не найдена
            lineNumberTextView.setText("");
        }
        stationNameTextView.setText(station.getName());
        stationNameTextView.setSelected(true);  // Добавлено
        stationNameTextView.requestFocus();     // Добавлено
        // В методе showStationInfoLayout после установки текста
        stationNameTextView.post(() -> {
            HorizontalScrollView scrollView = stationInfoView.findViewById(R.id.scrollView);
            int textWidth = stationNameTextView.getWidth();
            int containerWidth = scrollView.getWidth();

            if (textWidth > containerWidth) {
                int distance = textWidth - containerWidth;

                // Отменяем предыдущие анимации
                if (isStartStation && startStationAnimator != null) {
                    startStationAnimator.cancel();
                } else if (endStationAnimator != null) {
                    endStationAnimator.cancel();
                }

                // 1. Начальная пауза (1 секунда)
                ValueAnimator startPause = ValueAnimator.ofInt(0, 0).setDuration(3000);

                // 2. Плавная прокрутка вперед за ФИКСИРОВАННЫЕ 4 секунды
                ObjectAnimator scrollForward = ObjectAnimator.ofInt(scrollView, "scrollX", 0, distance);
                scrollForward.setDuration(2000); // Фиксированная длительность
                scrollForward.setInterpolator(new LinearInterpolator());

                // 3. Конечная пауза (1 секунда)
                ValueAnimator endPause = ValueAnimator.ofInt(0, 0).setDuration(1000);

                // 4. Быстрый возврат за 0.8 секунды
                ObjectAnimator scrollBack = ObjectAnimator.ofInt(scrollView, "scrollX", distance, 0);
                scrollBack.setDuration(800);
                scrollBack.setInterpolator(new AccelerateInterpolator(1.5f));

                AnimatorSet fullSequence = new AnimatorSet();
                fullSequence.playSequentially(
                        startPause,    // 1 сек
                        scrollForward, // 4 сек
                        endPause,      // 1 сек
                        scrollBack     // 0.8 сек
                );

                fullSequence.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fullSequence.start(); // Общая длительность цикла: 6.8 сек
                    }
                });


                fullSequence.start();
            }
        });
// В методе закрытия
        // обработчик выше объединяет отмену анимации и сброс выбора станции

        // Добавляем station_info_layout в контейнер
        container.addView(stationInfoView);

        // Добавляем горизонтальный индикатор числа страниц под карточкой, если у станции есть переходы
        List<Transfer> stationTransfers = new ArrayList<>();
        for (Transfer t : transfers) {
            if (t.getStations().contains(station)) stationTransfers.add(t);
        }
        // Скрываем TextInputEditText
        if (isStartStation) {
            startStationEditText.setVisibility(View.GONE);
            startStationEditText.setEnabled(false);
            startStationEditText.setHint("");
        } else {
            endStationEditText.setVisibility(View.GONE);
            endStationEditText.setEnabled(false);
            endStationEditText.setHint("");
        }
    }

    private void startLocationUpdates() {
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
        locationUpdateHandler.post(locationUpdateRunnable);
        }
    }

    private void stopLocationUpdates() {
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагружаем данные при возврате из настроек
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        
        // Очищаем старые данные перед загрузкой новых
        clearAllData();
        loadMetroData(selectedMapFileName);
        
        // Обновляем адаптер станций
        if (stationsAdapter != null) {
            stationsAdapter.setStations(stations);
        }
        
        // Скрываем список станций при возврате из настроек
        hideStationsList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Остановка обновления местоположения при уничтожении активности
        stopLocationUpdates();
        
        // Очищаем Handler
        if (locationUpdateHandler != null) {
            locationUpdateHandler = null;
        }
        locationUpdateRunnable = null;
    }

    private List<Transfer> transfers = new ArrayList<>();
    private List<River> rivers;
    private List<MapObject> mapObjects;
    private List<Transfer> suburbanTransfers;
    private List<River> suburbanRivers;
    private List<MapObject> suburbanMapObjects;
    private List<Line> allLines;

    public List<Line> getAllLines() {
        return allLines;
    }

    private void clearAllData() {
        // Очищаем все списки данных
        if (lines != null) lines.clear();
        if (stations != null) stations.clear();
        if (transfers != null) transfers.clear();
        if (rivers != null) rivers.clear();
        if (mapObjects != null) mapObjects.clear();
        
        if (suburbanLines != null) suburbanLines.clear();
        if (suburbanStations != null) suburbanStations.clear();
        if (suburbanTransfers != null) suburbanTransfers.clear();
        if (suburbanRivers != null) suburbanRivers.clear();
        if (suburbanMapObjects != null) suburbanMapObjects.clear();
        
        if (riverTramLines != null) riverTramLines.clear();
        if (riverTramStations != null) riverTramStations.clear();
        if (riverTramTransfers != null) riverTramTransfers.clear();
        if (riverTramRivers != null) riverTramRivers.clear();
        if (riverTramMapObjects != null) riverTramMapObjects.clear();
        if (tramLines != null) tramLines.clear();
        if (tramStations != null) tramStations.clear();
        if (tramTransfers != null) tramTransfers.clear();
        if (tramRivers != null) tramRivers.clear();
        if (tramMapObjects != null) tramMapObjects.clear();
        
        if (allLines != null) allLines.clear();
        
        // Очищаем выбранные станции
        if (selectedStations != null) selectedStations.clear();
        selectedStartStation = null;
        selectedEndStation = null;
        
        // Очищаем маршрут на карте
        if (metroMapView != null) {
            metroMapView.clearRoute();
        }
    }

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject metroMapData = jsonObject.optJSONObject("metro_map");
            JSONObject suburbanMapData = jsonObject.optJSONObject("suburban_map");
            JSONObject tramMapData = jsonObject.optJSONObject("tram_map");
            JSONObject riverTramMapData = jsonObject.optJSONObject("rivertram_map");

            // Загружаем данные для метро
            if (metroMapData != null) {
                loadMapData(metroMapData, lines, stations, transfers, rivers, mapObjects);
            }

            // Загружаем данные для электричек, если они есть
            if (suburbanMapData != null) {
                loadMapData(suburbanMapData, suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects);
            }

            // Загружаем данные для трамвая
            if (tramMapData != null) {
                loadMapData(tramMapData, tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects);
            }

            // Загружаем данные для речного трамвая
            if (riverTramMapData != null) {
                loadMapData(riverTramMapData, riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects);
            }

            // Объединяем станции и добавляем соседей
            List<Station> allStations = new ArrayList<>(stations);
            if (suburbanMapData != null) {
                allStations.addAll(suburbanStations);
            }
            if (riverTramMapData != null) {
                allStations.addAll(riverTramStations);
            }
            if (tramMapData != null) {
                allStations.addAll(tramStations);
            }
            if (metroMapData != null) {
                addNeighbors(metroMapData, allStations);
            }
            if (suburbanMapData != null) {
                addNeighbors(suburbanMapData, allStations);
            }
            if (tramMapData != null) {
                addNeighbors(tramMapData, allStations);
            }
            if (riverTramMapData != null) {
                addNeighbors(riverTramMapData, allStations);
            }

            allLines = new ArrayList<>(lines);
            if (suburbanMapData != null) {
                allLines.addAll(suburbanLines);
            }
            if (tramMapData != null) {
                allLines.addAll(tramLines);
            }
            if (riverTramMapData != null) {
                allLines.addAll(riverTramLines);
            }
            
            // Логируем все линии для отладки
            Log.d("MainActivity", "All lines count: " + allLines.size());
            for (Line l : allLines) {
                if (l != null) {
                    Log.d("MainActivity", "AllLines: id=" + l.getId() + ", displayNumber=" + l.getdisplayNumber() + ", name=" + l.getName());
                }
            }

            updateMapData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateMapData() {
        Log.d("MainActivity", "updateMapData called - isMetroMap: " + isMetroMap + ", isSuburbanMap: " + isSuburbanMap + ", isRiverTramMap: " + isRiverTramMap + ", isTramMap: " + isTramMap);
        Log.d("MainActivity", "Lines count: " + (lines != null ? lines.size() : "null"));
        Log.d("MainActivity", "Stations count: " + (stations != null ? stations.size() : "null"));
        
        if (isMetroMap) {
            Log.d("MainActivity", "Setting metro map data");
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    true, false, false, false
            );
        } else if (isSuburbanMap) {
            Log.d("MainActivity", "Setting suburban map data");
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, true, false, false
            );
        } else if (isRiverTramMap) {
            Log.d("MainActivity", "Setting river tram map data");
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, false, true, false
            );
        } else if (isTramMap) {
            Log.d("MainActivity", "Setting tram map data");
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, false, false, true
            );
        }
        
        // Принудительно обновляем отрисовку
        if (metroMapView != null) {
            metroMapView.invalidate();
        }
    }

    private void loadMapData(JSONObject mapData, List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) throws JSONException {
        if (lines == null) lines = new ArrayList<>();
        if (stations == null) stations = new ArrayList<>();
        if (transfers == null) transfers = new ArrayList<>();
        if (rivers == null) rivers = new ArrayList<>();
        if (mapObjects == null) mapObjects = new ArrayList<>();
        JSONArray linesArray = mapData.getJSONArray("lines");
        
        Log.d("MainActivity", "loadMapData - Lines array size: " + linesArray.length());

        for (int i = 0; i < linesArray.length(); i++) {
            JSONObject lineObject = linesArray.getJSONObject(i);
            boolean isCircle = lineObject.optBoolean("isCircle", false);
            String lineType = lineObject.optString("lineType", "single");
            String displayNumber = lineObject.optString("displayNumber", null);
            
            // Проверяем, что displayNumber не равен строке "null"
            if (displayNumber != null && displayNumber.equals("null")) {
                displayNumber = null;
            }
            
            // Оставляем displayNumber как есть из JSON, даже если он равен id
            // Логика фильтрации будет в местах отображения
            String displayShape = lineObject.optString("displayShape", null);
            Tariff tariff = createTariff(lineObject.optJSONObject("tariff"));
            
            Log.d("MainActivity", "Creating Line from JSON: id=" + lineObject.optString("id") + ", displayNumber=" + displayNumber + ", name=" + lineObject.optString("name"));
            
            Line line = new Line(lineObject.getString("id"), lineObject.getString("name"), lineObject.getString("color"), isCircle, lineType, tariff, displayNumber, displayShape);
            
            Log.d("MainActivity", "Loaded line: id=" + line.getId() + ", displayNumber=" + line.getdisplayNumber() + ", name=" + line.getName());
            JSONArray stationsArray = lineObject.getJSONArray("stations");
            
            Log.d("MainActivity", "Loading line: " + line.getName() + " with " + stationsArray.length() + " stations");
            for (int j = 0; j < stationsArray.length(); j++) {
                JSONObject stationObject = stationsArray.getJSONObject(j);
                String schedule = stationObject.optString("schedule", "5:30 - 0:00");
                int escalators = stationObject.optInt("escalators", 0);
                int elevators = stationObject.optInt("elevators", 0);
                String[] exits = toStringArray(stationObject.optJSONArray("exits"));
                int textPosition = 0;
                Integer labelX = null;
                Integer labelY = null;
                if (stationObject.has("textPosition")) {
                    Object textPositionValue = stationObject.opt("textPosition");
                    if (textPositionValue instanceof JSONObject) {
                        JSONObject textPositionObject = (JSONObject) textPositionValue;
                        if (textPositionObject.has("x") && textPositionObject.has("y")) {
                            labelX = textPositionObject.optInt("x");
                            labelY = textPositionObject.optInt("y");
                        }
                        if (textPositionObject.has("position")) {
                            textPosition = textPositionObject.optInt("position", 0);
                        } else {
                            textPosition = 0;
                        }
                    } else {
                        textPosition = stationObject.optInt("textPosition", 0);
                    }
                }
                String ESP = stationObject.optString("ESP", null); // Добавляем поле ESP
                // Загружаем широту и долготу (поддерживаем оба формата)
                double latitude = stationObject.optDouble("latitude", stationObject.optDouble("lat", 0.0));
                double longitude = stationObject.optDouble("longitude", stationObject.optDouble("lon", 0.0));
                Facilities facilities = new Facilities(schedule, escalators, elevators, exits);
                Station station = new Station(
                        stationObject.getString("id"),
                        stationObject.getString("name"),
                        stationObject.getInt("x"),
                        stationObject.getInt("y"),
                        ESP,
                        line.getColor(),
                        facilities,
                        textPosition
                );
                if (labelX != null && labelY != null) {
                    station.setLabelCoordinates(labelX, labelY);
                }

                // Устанавливаем широту и долготу
                if (latitude != 0.0 && longitude != 0.0 && !Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                    station.setLatitude(latitude);
                    station.setLongitude(longitude);
                }

                stations.add(station);
                line.getStations().add(station);
            }
            lines.add(line);
            Log.d("MainActivity", "Added line: " + line.getName() + " with " + line.getStations().size() + " stations");
        }

        Log.d("MainActivity", "Total lines loaded: " + lines.size());
        Log.d("MainActivity", "Total stations loaded: " + stations.size());

        // Первый проход: создаём переходы по ID и станциям (пока без разрешения ссылок TR_*)
        JSONArray transfersArray = mapData.getJSONArray("transfers");
        Map<String, Transfer> idToTransfer = new HashMap<>();
        List<JSONObject> unresolvedLinkTransfers = new ArrayList<>();
        for (int i = 0; i < transfersArray.length(); i++) {
            JSONObject transferObject = transfersArray.getJSONObject(i);
            String transferId = transferObject.optString("id", null);
            JSONArray stationsArray = transferObject.getJSONArray("stations");
            List<Station> transferStations = new ArrayList<>();
            boolean hasLink = false;
            List<String> linkedStationIds = new ArrayList<>();
            for (int j = 0; j < stationsArray.length(); j++) {
                String token = stationsArray.getString(j);
                if (token.startsWith("TR_")) { hasLink = true; }
                if (!token.startsWith("TR_")) linkedStationIds.add(token);
                Station station = findStationById(token, stations);
                if (station != null) transferStations.add(station);
            }
            int time = transferObject.optInt("time", 3);
            String type = transferObject.optString("type", "regular");
            String transferMap = transferObject.optString("transfer_map", null);

            List<TransferRoute> transferRoutes = new ArrayList<>();
            if (transferObject.has("transfers_routes")) {
                JSONArray transferRoutesArray = transferObject.getJSONArray("transfers_routes");
                for (int k = 0; k < transferRoutesArray.length(); k++) {
                    JSONObject routeObject = transferRoutesArray.getJSONObject(k);
                    String routeTransferMap = routeObject.optString("transfer_map", null);
                    String prev = routeObject.optString("prev", null);
                    String from = routeObject.optString("from", null);
                    String to = routeObject.optString("to", null);
                    JSONArray wayArray = routeObject.optJSONArray("way");
                    List<String> way = new ArrayList<>();
                    if (wayArray != null) {
                        for (int l = 0; l < wayArray.length(); l++) way.add(wayArray.getString(l));
                    }
                    transferRoutes.add(new TransferRoute(routeTransferMap, from, to, way, prev, null));
                }
            }

            if (hasLink) {
                // Сохраняем на второй проход — нужно разрешить TR_* в станции
                unresolvedLinkTransfers.add(transferObject);
            } else {
                Transfer t = new Transfer(transferId, transferStations, time, type, transferMap, transferRoutes);
                transfers.add(t);
                if (transferId != null) idToTransfer.put(transferId, t);
            }
        }

        // Второй проход: разрешаем ссылки TR_* в списках stations
        for (JSONObject transferObject : unresolvedLinkTransfers) {
            String transferId = transferObject.optString("id", null);
            JSONArray stationsArray = transferObject.getJSONArray("stations");
            List<Station> combined = new ArrayList<>();
            List<String> linkedIds = new ArrayList<>();
            List<String> linkedStationIds = new ArrayList<>();
            boolean hasLinks = false;
            
            for (int j = 0; j < stationsArray.length(); j++) {
                String token = stationsArray.getString(j);
                if (token.startsWith("TR_")) {
                    hasLinks = true;
                    linkedIds.add(token);
                    Transfer ref = idToTransfer.get(token);
                    if (ref != null && ref.getStations() != null) combined.addAll(ref.getStations());
                } else {
                    Station station = findStationById(token, stations);
                    if (station != null) {
                        combined.add(station);
                        linkedStationIds.add(token);
                    }
                }
            }
            int time = transferObject.optInt("time", 3);
            String type = transferObject.optString("type", "regular");
            String transferMap = transferObject.optString("transfer_map", null);
            List<TransferRoute> transferRoutes = new ArrayList<>();
            if (transferObject.has("transfers_routes")) {
                JSONArray transferRoutesArray = transferObject.getJSONArray("transfers_routes");
                for (int k = 0; k < transferRoutesArray.length(); k++) {
                    JSONObject routeObject = transferRoutesArray.getJSONObject(k);
                    String routeTransferMap = routeObject.optString("transfer_map", null);
                    String prev = routeObject.optString("prev", null);
                    String from = routeObject.optString("from", null);
                    String to = routeObject.optString("to", null);
                    JSONArray wayArray = routeObject.optJSONArray("way");
                    List<String> way = new ArrayList<>();
                    if (wayArray != null) {
                        for (int l = 0; l < wayArray.length(); l++) way.add(wayArray.getString(l));
                    }
                    transferRoutes.add(new TransferRoute(routeTransferMap, from, to, way, prev, null));
                }
            }
            Transfer t = new Transfer(transferId, combined, time, type, transferMap, transferRoutes, hasLinks, linkedIds, linkedStationIds);
            transfers.add(t);
            if (transferId != null) idToTransfer.put(transferId, t);
        }


        if (mapData.has("rivers")) { // Проверяем, есть ли ключ "rivers" в JSON
            JSONArray riversArray = mapData.getJSONArray("rivers");
            for (int i = 0; i < riversArray.length(); i++) {
                JSONObject riverObject = riversArray.getJSONObject(i);

                // Проверяем, есть ли ключ "points" в объекте реки
                if (riverObject.has("points")) {
                    JSONArray pointsArray = riverObject.getJSONArray("points");
                    List<Point> riverPoints = new ArrayList<>();

                    // Парсим точки реки
                    for (int j = 0; j < pointsArray.length(); j++) {
                        JSONObject pointObject = pointsArray.getJSONObject(j);

                        // Проверяем, есть ли ключи "x" и "y" в объекте точки
                        if (pointObject.has("x") && pointObject.has("y")) {
                            int x = pointObject.getInt("x");
                            int y = pointObject.getInt("y");
                            Point point = new Point(x, y);
                            riverPoints.add(point);
                        } else {
                            Log.e("RiverParsing", "Missing 'x' or 'y' in point object at index " + j);
                        }
                    }

                    // Парсим ширину реки (если есть, иначе используем значение по умолчанию)
                    int width = riverObject.optInt("width", 10); // Значение по умолчанию: 10

                    // Создаем объект River и добавляем его в список рек
                    rivers.add(new River(riverPoints, width));
                } else {
                    Log.e("RiverParsing", "Missing 'points' array in river object at index " + i);
                }
            }
        } else {
            Log.e("RiverParsing", "No 'rivers' array found in map data");
        }

        JSONArray intermediatePointsArray = mapData.getJSONArray("intermediatePoints");
        for (int i = 0; i < intermediatePointsArray.length(); i++) {
            JSONObject intermediatePointObject = intermediatePointsArray.getJSONObject(i);
            JSONArray neighborsIdArray = intermediatePointObject.getJSONArray("neighborsId");
            String station1Id = neighborsIdArray.getString(0);
            String station2Id = neighborsIdArray.getString(1);

            Station station1 = findStationById(station1Id, stations);
            Station station2 = findStationById(station2Id, stations);

            if (station1 != null && station2 != null) {
                JSONArray pointsArray = intermediatePointObject.getJSONArray("points");
                List<Point> intermediatePoints = new ArrayList<>();
                for (int j = 0; j < pointsArray.length(); j++) {
                    JSONObject pointObject = pointsArray.getJSONObject(j);
                    Point point = new Point(pointObject.getInt("x"), pointObject.getInt("y"));
                    intermediatePoints.add(point);
                }
                station1.addIntermediatePoints(station2, intermediatePoints);
            }
        }

        JSONArray objectsArray = mapData.getJSONArray("objects");
        for (int i = 0; i < objectsArray.length(); i++) {
            JSONObject objectObject = objectsArray.getJSONObject(i);
            String name = objectObject.getString("name");
            String displayNumber = objectObject.getString("displayNumber");
            String type = objectObject.getString("type");
            JSONObject positionObject = objectObject.getJSONObject("position");
            Point position = new Point(positionObject.getInt("x"), positionObject.getInt("y"));
            mapObjects.add(new MapObject(name, type, position, displayNumber));
        }
    }

    private Tariff createTariff(JSONObject tariffObject) throws JSONException {
        if (tariffObject == null) return null;

        String type = tariffObject.getString("type");
        switch (type) {
            case "FlatRateTariff":
                double price = tariffObject.getDouble("price");
                return new FlatRateTariff(price);
            case "ZoneBasedTariff":
                Map<Integer, Double> zonePrices = new HashMap<>();
                JSONObject zonePricesObject = tariffObject.getJSONObject("zonePrices");
                Iterator<String> keys = zonePricesObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    zonePrices.put(Integer.parseInt(key), zonePricesObject.getDouble(key));
                }
                return new ZoneBasedTariff(zonePrices);
            case "APITariff":
                return new APITariff();
            default:
                return null;
        }
    }

    private void addNeighbors(JSONObject mapData, List<Station> allStations) throws JSONException {
        JSONArray linesArray = mapData.getJSONArray("lines");

        for (int i = 0; i < linesArray.length(); i++) {
            JSONObject lineObject = linesArray.getJSONObject(i);
            JSONArray stationsArray = lineObject.getJSONArray("stations");
            for (int j = 0; j < stationsArray.length(); j++) {
                JSONObject stationObject = stationsArray.getJSONObject(j);
                Station station = findStationById(stationObject.getString("id"), allStations);
                if (station != null) {
                    if (stationObject.has("neighbors")) {
                    JSONArray neighborsArray = stationObject.getJSONArray("neighbors");
                        Log.d("MainActivity", "Station " + station.getName() + " has " + neighborsArray.length() + " neighbors");
                    for (int k = 0; k < neighborsArray.length(); k++) {
                        JSONArray neighborArray = neighborsArray.getJSONArray(k);
                        String neighborId = neighborArray.getString(0);
                        int time = neighborArray.getInt(1);
                        Station neighborStation = findStationById(neighborId, allStations);
                        if (neighborStation != null) {
                            station.addNeighbor(new Station.Neighbor(neighborStation, time));
                                Log.d("MainActivity", "Added neighbor: " + neighborStation.getName() + " to " + station.getName());
                            } else {
                                Log.w("MainActivity", "Neighbor station not found: " + neighborId);
                            }
                        }
                    } else {
                        Log.w("MainActivity", "Station " + station.getName() + " has no neighbors field");
                    }

                    // Для кольцевых линий обозначим связь первой и последней станций (если есть флаг в JSON)
                    boolean isCircle = stationObject.optBoolean("_belongsToCircle", false);
                    if (isCircle && stationsArray.length() > 1) {
                        Station first = findStationById(stationsArray.getJSONObject(0).getString("id"), allStations);
                        Station last = findStationById(stationsArray.getJSONObject(stationsArray.length()-1).getString("id"), allStations);
                        if (first != null && last != null) {
                            // Добавим взаимных соседей, если их нет
                            boolean hasFirstLast = false;
                            for (Station.Neighbor n : first.getNeighbors()) {
                                if (n.getStation().getId().equals(last.getId())) { hasFirstLast = true; break; }
                            }
                            if (!hasFirstLast) {
                                first.addNeighbor(new Station.Neighbor(last, 2));
                            }
                            boolean hasLastFirst = false;
                            for (Station.Neighbor n : last.getNeighbors()) {
                                if (n.getStation().getId().equals(first.getId())) { hasLastFirst = true; break; }
                            }
                            if (!hasLastFirst) {
                                last.addNeighbor(new Station.Neighbor(first, 2));
                            }
                        }
                    }
                }
            }
        }
    }

    private String[] toStringArray(JSONArray array) {
        if (array == null) return new String[0];
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            try {
                result[i] = array.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private String loadJSONFromAsset(String mapFileName) {
        String json = null;
        try {
            InputStream is = getAssets().open("raw/" + mapFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private Station findStationById(String id, List<Station> stations) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }

    @Override
    public void onStationClick(Station station) {
        Log.d("MainActivity", "Station clicked: " + station.getName());
        Station prevStation = null;
        Station nextStation = null;
        Line curline = null;

        // Получаем список линий в зависимости от активной карты
        List<Line> activeLines = getActiveLines();

        // Поиск линии и соседних станций
        for (Line line : activeLines) {
            List<Station> lineStations = line.getStations();
            for (int i = 0; i < lineStations.size(); i++) {
                if (lineStations.get(i).equals(station)) {
                    if (i > 0) {
                        prevStation = lineStations.get(i - 1);
                    }
                    if (i < lineStations.size() - 1) {
                        nextStation = lineStations.get(i + 1);
                    }
                    curline = line;
                    Log.d("MainActivity", "Line found: " + line.getName());
                    // Обработка кольцевых линий: замыкаем начало и конец
                    if (curline != null && curline.isCircle() && lineStations.size() > 1) {
                        if (prevStation == null) {
                            prevStation = lineStations.get(lineStations.size() - 1);
                        }
                        if (nextStation == null) {
                            nextStation = lineStations.get(0);
                        }
                    }
                    break;
                }
            }
        }

        // Очистка предыдущего фрагмента
        clearFrameLayout();

        // Создание и установка адаптера для ViewPager2
        List<Line> grayedLines = new ArrayList<>();
        
        // Добавляем неактивные линии как серые
        if (isMetroMap) {
            if (suburbanLines != null) grayedLines.addAll(suburbanLines);
            if (riverTramLines != null) grayedLines.addAll(riverTramLines);
            if (tramLines != null) grayedLines.addAll(tramLines);
        } else if (isSuburbanMap) {
            if (lines != null) grayedLines.addAll(lines);
            if (riverTramLines != null) grayedLines.addAll(riverTramLines);
            if (tramLines != null) grayedLines.addAll(tramLines);
        } else if (isRiverTramMap) {
            if (lines != null) grayedLines.addAll(lines);
            if (suburbanLines != null) grayedLines.addAll(suburbanLines);
            if (tramLines != null) grayedLines.addAll(tramLines);
        } else if (isTramMap) {
            if (lines != null) grayedLines.addAll(lines);
            if (suburbanLines != null) grayedLines.addAll(suburbanLines);
            if (riverTramLines != null) grayedLines.addAll(riverTramLines);
        }
        
        // Получаем активные переходы
        List<Transfer> activeTransfers = getActiveTransfers();
        
        StationPagerAdapter pagerAdapter = new StationPagerAdapter(
                this, station, curline, prevStation, nextStation,
                activeTransfers, activeLines, grayedLines, this
        );
        currentPagerAdapter = pagerAdapter;

        ViewPager2 stationPager = findViewById(R.id.stationPager);
        stationPager.setAdapter(pagerAdapter);
        stationPager.setVisibility(View.VISIBLE); // Показываем ViewPager2
        addHorizontalPagerDots(stationPager, pagerAdapter);
        metroMapView.selectedStation = station;
        metroMapView.invalidate();
    }

    private void addHorizontalPagerDots(ViewPager2 pager, StationPagerAdapter adapter) {
        int count = adapter != null ? adapter.getItemCount() : 0;
        LinearLayout dotsHost = findViewById(R.id.stationPagerDots);
        if (dotsHost == null) return;
        dotsHost.removeAllViews();

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dots.setLayoutParams(lp);

        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        int dp4 = (int) (4 * getResources().getDisplayMetrics().density);

        if (dotsHost.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) dotsHost.getLayoutParams();
            params.topToBottom = R.id.stationPager;
            params.bottomToTop = R.id.linearLayout2;
            params.matchConstraintMaxHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;
            dotsHost.setLayoutParams(params);
        }
        if (stationPagerChangeCallback != null) {
            pager.unregisterOnPageChangeCallback(stationPagerChangeCallback);
            stationPagerChangeCallback = null;
        }

        // Скрываем индикатор если одна страница или 0
        if (count <= 1) {
            dotsHost.setVisibility(View.GONE);
            return;
        } else {
            dotsHost.setVisibility(View.VISIBLE);
        }

        // Получаем цвет primary из темы
        int primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#1976D2"));
        
        for (int i = 0; i < count; i++) {
            View v = new View(this);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(dp8, dp8);
            vp.setMargins(dp4, 0, dp4, 0);
            v.setLayoutParams(vp);
            if (i == 0) {
                // Создаем drawable программно для активной точки с цветом из темы
                android.graphics.drawable.GradientDrawable selectedDot = new android.graphics.drawable.GradientDrawable();
                selectedDot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                selectedDot.setSize(dp8, dp8);
                selectedDot.setColor(primaryColor);
                v.setBackground(selectedDot);
            } else {
                v.setBackgroundResource(R.drawable.dot_unselected);
            }
            dots.addView(v);
        }

        dotsHost.addView(dots);

        stationPagerChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.getChildCount(); i++) {
                    if (i == position) {
                        // Создаем drawable программно для активной точки с цветом из темы
                        android.graphics.drawable.GradientDrawable selectedDot = new android.graphics.drawable.GradientDrawable();
                        selectedDot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        selectedDot.setSize(dp8, dp8);
                        selectedDot.setColor(primaryColor);
                        dots.getChildAt(i).setBackground(selectedDot);
                    } else {
                        dots.getChildAt(i).setBackgroundResource(R.drawable.dot_unselected);
                    }
                }

                Station currentStation = adapter.getStationAtPosition(position);
                if (currentStation != null) {
                    metroMapView.selectedStation = currentStation;
                    metroMapView.invalidate();
                }

                Line currentLine = adapter.getLineAtPosition(position);
                if (currentLine != null) {
                    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    String fragmentTag = "f" + pager.getId() + ":" + position;
                    androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
                    if (fragment instanceof com.nicorp.nimetro.presentation.fragments.StationInfoFragment) {
                        ((com.nicorp.nimetro.presentation.fragments.StationInfoFragment) fragment).updateLineNumber(currentLine);
                    }
                }
            }
        };
        pager.registerOnPageChangeCallback(stationPagerChangeCallback);
    }

    private void addRoutePagerDots(ViewPager2 pager, RoutePagerAdapter adapter) {
        int count = adapter != null ? adapter.getItemCount() : 0;
        LinearLayout dotsHost = findViewById(R.id.routePagerDots);
        if (dotsHost == null) return;
        dotsHost.removeAllViews();

        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dots.setLayoutParams(lp);

        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        int dp4 = (int) (4 * getResources().getDisplayMetrics().density);

        if (dotsHost.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) dotsHost.getLayoutParams();
            params.topToBottom = R.id.frameLayout;
            params.bottomToTop = R.id.linearLayout2;
            params.matchConstraintMaxHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;
            dotsHost.setLayoutParams(params);
        }
        if (routePagerChangeCallback != null) {
            pager.unregisterOnPageChangeCallback(routePagerChangeCallback);
            routePagerChangeCallback = null;
        }

        if (count <= 1) {
            dotsHost.setVisibility(View.GONE);
            return;
        } else {
            dotsHost.setVisibility(View.VISIBLE);
        }

        int primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#1976D2"));
        
        for (int i = 0; i < count; i++) {
            View v = new View(this);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(dp8, dp8);
            vp.setMargins(dp4, 0, dp4, 0);
            v.setLayoutParams(vp);
            if (i == 0) {
                android.graphics.drawable.GradientDrawable selectedDot = new android.graphics.drawable.GradientDrawable();
                selectedDot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                selectedDot.setSize(dp8, dp8);
                selectedDot.setColor(primaryColor);
                v.setBackground(selectedDot);
            } else {
                v.setBackgroundResource(R.drawable.dot_unselected);
            }
            dots.addView(v);
        }

        dotsHost.addView(dots);

        routePagerChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.getChildCount(); i++) {
                    if (i == position) {
                        android.graphics.drawable.GradientDrawable selectedDot = new android.graphics.drawable.GradientDrawable();
                        selectedDot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                        selectedDot.setSize(dp8, dp8);
                        selectedDot.setColor(primaryColor);
                        dots.getChildAt(i).setBackground(selectedDot);
                    } else {
                        dots.getChildAt(i).setBackgroundResource(R.drawable.dot_unselected);
                    }
                }

                List<RouteStation> selectedRoute = adapter.getRouteAtPosition(position);
                if (selectedRoute != null && metroMapView != null) {
                    metroMapView.setRouteFromRouteStations(selectedRoute);
                    metroMapView.invalidate();
                }
            }
        };
        pager.registerOnPageChangeCallback(routePagerChangeCallback);
    }

    // Метод для получения активных линий в зависимости от текущей карты
    private List<Line> getActiveLines() {
        if (isMetroMap) {
            return lines != null ? lines : new ArrayList<>();
        } else if (isSuburbanMap) {
            return suburbanLines != null ? suburbanLines : new ArrayList<>();
        } else if (isRiverTramMap) {
            return riverTramLines != null ? riverTramLines : new ArrayList<>();
        } else if (isTramMap) {
            return tramLines != null ? tramLines : new ArrayList<>();
        }
        return new ArrayList<>(); // Возвращаем пустой список, если ни одна карта не активна
    }

    // Метод для получения активных переходов в зависимости от текущей карты
    private List<Transfer> getActiveTransfers() {
        if (isMetroMap) {
            return transfers != null ? transfers : new ArrayList<>();
        } else if (isSuburbanMap) {
            return suburbanTransfers != null ? suburbanTransfers : new ArrayList<>();
        } else if (isRiverTramMap) {
            return riverTramTransfers != null ? riverTramTransfers : new ArrayList<>();
        } else if (isTramMap) {
            return tramTransfers != null ? tramTransfers : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    @Override
    public void onDismiss() {
        hideStationPager();
    }

    private void clearFrameLayout() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.frameLayout);
        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
        FrameLayout frameLayout = findViewById(R.id.frameLayout);
        if (frameLayout != null) {
            frameLayout.removeAllViews();
        }
        LinearLayout routePagerDots = findViewById(R.id.routePagerDots);
        if (routePagerDots != null) {
            routePagerDots.setVisibility(View.GONE);
            routePagerDots.removeAllViews();
        }
        if (routePagerChangeCallback != null) {
            routePagerChangeCallback = null;
        }
        currentRoutePagerAdapter = null;
    }

    public void dismissRouteInfo() {
        clearFrameLayout();
    }

    private void resetStationPagerIfNoSelection() {
        if (selectedStartStation != null || selectedEndStation != null) {
            return;
        }
        hideStationPager();
    }

    private void hideStationPager() {
        ViewPager2 stationPager = findViewById(R.id.stationPager);
        if (stationPager != null) {
            if (stationPagerChangeCallback != null) {
                stationPager.unregisterOnPageChangeCallback(stationPagerChangeCallback);
                stationPagerChangeCallback = null;
            }
            stationPager.setAdapter(null);
            stationPager.setVisibility(View.GONE);
            currentPagerAdapter = null;
        }
        LinearLayout dotsHost = findViewById(R.id.stationPagerDots);
        if (dotsHost != null) {
            dotsHost.removeAllViews();
            dotsHost.setVisibility(View.GONE);
            if (dotsHost.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) dotsHost.getLayoutParams();
                params.matchConstraintMaxHeight = 0;
                params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                params.bottomToTop = ConstraintLayout.LayoutParams.UNSET;
                dotsHost.setLayoutParams(params);
            }
        }
    }

    private Station getCurrentStation() {
        ViewPager2 stationPager = findViewById(R.id.stationPager);
        if (stationPager != null && currentPagerAdapter != null) {
            int currentPosition = stationPager.getCurrentItem();
            return currentPagerAdapter.getStationAtPosition(currentPosition);
        }
        return null;
    }

    @Override
    public void onSetStart(Station station, Line line, boolean fromStationInfoFragment) {
        Line lineToUse = line;
        if (lineToUse == null && selectedEndStation != null) {
            lineToUse = findOptimalLineForRoute(station, selectedEndStation);
        }
        
        Station stationToUse = findStationInLine(station, lineToUse);
        if (stationToUse == null) {
            stationToUse = station;
        }
        
        if (selectedStartStation != null) {
            selectedStations.remove(selectedStartStation);
        }
        selectedStartStation = stationToUse;
        selectedStations.add(stationToUse);
        metroMapView.setSelectedStations(selectedStations);
        setStationInfo(startStationEditText, stationToUse);
        startStationEditText.setHint("");
        startStationEditText.clearFocus();
        startStationEditText.setEnabled(false);

        // Показываем station_info_layout для начальной станции
        showStationInfoLayout(stationToUse, true);

        rebuildRouteIfPossible();

        if (fromStationInfoFragment) {
            hideStationsList();
        }
    }

    @Override
    public void onSetEnd(Station station, Line line, boolean fromStationInfoFragment) {
        Station stationToUse = findStationInLine(station, line);
        if (stationToUse == null) {
            stationToUse = station;
        }
        
        if (selectedEndStation != null) {
            selectedStations.remove(selectedEndStation);
        }
        selectedEndStation = stationToUse;
        selectedStations.add(stationToUse);
        metroMapView.setSelectedStations(selectedStations);
        setStationInfo(endStationEditText, stationToUse);
        endStationEditText.setHint("");
        endStationEditText.clearFocus();
        endStationEditText.setEnabled(false);

        // Показываем station_info_layout для конечной станции
        showStationInfoLayout(stationToUse, false);

        rebuildRouteIfPossible();

        if (fromStationInfoFragment) {
            hideStationsList();
        }
    }
    
    private List<Line> findAllLinesForStation(Station station) {
        List<Line> result = new ArrayList<>();
        if (station == null || station.getId() == null) {
            return result;
        }
        
        String stationId = station.getId();
        Set<String> seenLineIds = new LinkedHashSet<>();
        
        List<Line> allLines = new ArrayList<>();
        if (lines != null) allLines.addAll(lines);
        if (suburbanLines != null) allLines.addAll(suburbanLines);
        if (riverTramLines != null) allLines.addAll(riverTramLines);
        if (tramLines != null) allLines.addAll(tramLines);
        
        for (Line line : allLines) {
            if (line == null || line.getStations() == null || line.getId() == null) {
                continue;
            }
            if (seenLineIds.contains(line.getId())) {
                continue;
            }
            for (Station lineStation : line.getStations()) {
                if (lineStation != null && lineStation.getId() != null && lineStation.getId().equals(stationId)) {
                    result.add(line);
                    seenLineIds.add(line.getId());
                    break;
                }
            }
        }
        
        return result;
    }

    private Line findOptimalLineForRoute(Station startStation, Station endStation) {
        if (startStation == null || endStation == null || startStation.getId() == null || endStation.getId() == null) {
            return null;
        }
        
        List<Line> allLinesForStation = findAllLinesForStation(startStation);
        if (allLinesForStation.isEmpty()) {
            return null;
        }
        
        if (allLinesForStation.size() == 1) {
            return allLinesForStation.get(0);
        }
        
        Line optimalLine = null;
        int minTransfers = Integer.MAX_VALUE;
        
        for (Line line : allLinesForStation) {
            Station stationInLine = findStationInLine(startStation, line);
            if (stationInLine == null) {
                continue;
            }
            
            List<RouteStation> route = findOptimalRoute(stationInLine, endStation);
            if (route == null || route.isEmpty()) {
                continue;
            }
            
            int transfers = countTransfersInRoute(route);
            if (transfers < minTransfers) {
                minTransfers = transfers;
                optimalLine = line;
            }
        }
        
        if (optimalLine == null && !allLinesForStation.isEmpty()) {
            optimalLine = allLinesForStation.get(0);
        }
        
        return optimalLine;
    }

    private Station findStationInLine(Station station, Line line) {
        if (station == null || line == null || station.getId() == null) {
            return null;
        }
        
        List<Station> lineStations = line.getStations();
        if (lineStations == null) {
            return null;
        }
        
        String stationId = station.getId();
        for (Station lineStation : lineStations) {
            if (lineStation != null && lineStation.getId() != null && lineStation.getId().equals(stationId)) {
                return lineStation;
            }
        }
        
        return null;
    }

    private FusedLocationProviderClient fusedLocationClient;

    private void requestLocation() {
        Log.d("MainActivity", "Requesting location permission");
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double userLatitude = location.getLatitude();
                            double userLongitude = location.getLongitude();

                            // Находим ближайшую станцию
                            Station nearestStation = findNearestStation(userLatitude, userLongitude);

                            if (nearestStation != null) {
                                // Устанавливаем ближайшую станцию как начальную
                                onSetStart(nearestStation, null, false);
                            }
                        }
                    }
                });
    }

    private Station findNearestStation(double userLatitude, double userLongitude) {
        Station nearestStation = null;
        double minDistance = Double.MAX_VALUE;

        // Объединяем все станции (метро, электрички, речной трамвай)
        List<Station> allStations = new ArrayList<>(stations);
        allStations.addAll(suburbanStations);
        allStations.addAll(riverTramStations);

        for (Station station : allStations) {
            if (station.getLatitude() != 0.0 && station.getLongitude() != 0.0) {
                double distance = station.distanceTo(userLatitude, userLongitude);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestStation = station;
                }
            }
        }

        if (nearestStation != null) {
            metroMapView.selectedStation = nearestStation;
            metroMapView.centerOnStation(nearestStation);
        }

        return nearestStation;
    }

    private interface RouteEdgeCostStrategy {
        int getEdgeCost(Station currentStation, Station.Neighbor neighbor);
    }

    private List<RouteStation> findOptimalRoute(Station start, Station end) {
        Log.d("MainActivity", "Finding optimal route from " + start.getName() + " to " + end.getName());
        return findRouteWithStrategy(start, end, this::getFastestEdgeCost);
    }

    private List<RouteStation> findRouteWithFewTransfers(Station start, Station end) {
        Log.d("MainActivity", "Finding few-transfers route from " + start.getName() + " to " + end.getName());
        return findRouteWithStrategy(start, end, this::getFewTransfersEdgeCost);
    }

    private List<RouteStation> findRouteWithStrategy(Station start, Station end, RouteEdgeCostStrategy edgeCostStrategy) {
        Log.d("ROUTE_INFO", "=== МАРШРУТ: " + start.getName() + " -> " + end.getName() + " ===");
        
        List<Station> allStations = buildAllStationsForRouting();
        Map<Station, List<Station.Neighbor>> adjacency = buildRoutingAdjacency(allStations);

        // Используем RouteStation для учета линий
        Map<RouteStation, RouteStation> previous = new HashMap<>();
        Map<RouteStation, Integer> distances = new HashMap<>();
        PriorityQueue<RouteStation> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));
        
        // Определяем все линии для начальной станции
        List<Line> allLinesForStart = findAllLinesForStation(start);
        
        String startAdjacentInfo = "";
        if (allLinesForStart.size() > 1) {
            StringBuilder linesList = new StringBuilder();
            for (int i = 0; i < allLinesForStart.size(); i++) {
                if (i > 0) linesList.append(", ");
                String lineName = allLinesForStart.get(i).getName() != null ? 
                    allLinesForStart.get(i).getName() : allLinesForStart.get(i).getId();
                linesList.append(lineName);
            }
            startAdjacentInfo = " [СМЕЖНАЯ: " + linesList.toString() + "]";
        }
        
        // Инициализируем расстояния для всех комбинаций Station+Line
        for (Station station : allStations) {
            List<Line> linesForStation = findAllLinesForStation(station);
            if (linesForStation.isEmpty()) {
                RouteStation rs = new RouteStation(station, null);
                distances.put(rs, Integer.MAX_VALUE);
            } else {
                for (Line line : linesForStation) {
                    Station stationInLine = findStationInLine(station, line);
                    if (stationInLine == null) {
                        stationInLine = station;
                    }
                    RouteStation rs = new RouteStation(stationInLine, line);
                    distances.put(rs, Integer.MAX_VALUE);
                }
            }
        }
        
        // Для смежной начальной станции создаем RouteStation для всех доступных линий
        // и добавляем их все в очередь с расстоянием 0
        if (allLinesForStart.isEmpty()) {
            RouteStation startRouteStation = new RouteStation(start, null);
            distances.put(startRouteStation, 0);
            previous.put(startRouteStation, null);
            queue.add(startRouteStation);
            Log.d("ROUTE_INFO", "НАЧАЛО: " + start.getName() + " (ID: " + start.getId() + 
                  "), линия: null" + startAdjacentInfo);
        } else {
            for (Line startLine : allLinesForStart) {
                Station startStationInLine = findStationInLine(start, startLine);
                if (startStationInLine == null) {
                    startStationInLine = start;
                }
                RouteStation startRouteStation = new RouteStation(startStationInLine, startLine);
                distances.put(startRouteStation, 0);
                previous.put(startRouteStation, null);
                queue.add(startRouteStation);
                Log.d("ROUTE_INFO", "НАЧАЛО: " + startStationInLine.getName() + " (ID: " + startStationInLine.getId() + 
                      "), линия: " + (startLine.getName() != null ? startLine.getName() : startLine.getId()) + startAdjacentInfo);
            }
        }

        RouteStation endRouteStation = null;

        while (!queue.isEmpty()) {
            RouteStation current = queue.poll();
            if (current == null) {
                break;
            }
            
            Station currentStation = current.getStation();
            Line currentLine = current.getLine();
            int currentDistance = distances.get(current);
            
            // Для смежных станций используем станцию из текущей линии для поиска neighbors
            // Это важно, потому что neighbors могут быть определены для станции на конкретной линии
            Station currentStationInLine = currentStation;
            if (currentLine != null) {
                Station stationInLine = findStationInLine(currentStation, currentLine);
                if (stationInLine != null) {
                    currentStationInLine = stationInLine;
                }
            }
            
            // Проверяем, является ли текущая станция смежной (есть на нескольких линиях)
            List<Line> allLinesForCurrent = findAllLinesForStation(currentStation);
            boolean isCurrentAdjacent = allLinesForCurrent.size() > 1;
            String adjacentInfo = "";
            if (isCurrentAdjacent) {
                StringBuilder linesList = new StringBuilder();
                for (int i = 0; i < allLinesForCurrent.size(); i++) {
                    if (i > 0) linesList.append(", ");
                    String lineName = allLinesForCurrent.get(i).getName() != null ? 
                        allLinesForCurrent.get(i).getName() : allLinesForCurrent.get(i).getId();
                    linesList.append(lineName);
                }
                adjacentInfo = " [СМЕЖНАЯ: " + linesList.toString() + "]";
            }
            
            Log.d("ROUTE_INFO", "СТАНЦИЯ: " + currentStationInLine.getName() + " (ID: " + currentStationInLine.getId() + 
                  "), линия: " + (currentLine != null ? (currentLine.getName() != null ? currentLine.getName() : currentLine.getId()) : "null") +
                  ", расстояние: " + currentDistance + adjacentInfo);
            
            if (currentStation.getId().equals(end.getId())) {
                endRouteStation = current;
                Log.d("ROUTE_INFO", ">>> КОНЕЦ: " + end.getName() + " <<<");
                break;
            }
            
            // Используем currentStationInLine для поиска neighbors, чтобы получить neighbors для станции на текущей линии
            List<Station.Neighbor> neighbors = adjacency.get(currentStationInLine);
            // Если neighbors не найдены для станции на текущей линии, пробуем найти для исходной станции
            if (neighbors == null || neighbors.isEmpty()) {
                neighbors = adjacency.get(currentStation);
            }
            if (neighbors == null) {
                continue;
            }
            
            for (Station.Neighbor neighbor : neighbors) {
                Station neighborStation = neighbor.getStation();
                if (neighborStation == null) {
                    continue;
                }
                
                // Проверяем, является ли соседняя станция смежной (есть на нескольких линиях)
                List<Line> allLinesForNeighbor = findAllLinesForStation(neighborStation);
                boolean isAdjacentStation = allLinesForNeighbor.size() > 1;
                
                // Определяем тип связи
                // ВАЖНО: проверяем neighbors в adjacency графе, который уже содержит все neighbors-связи
                // Если связь есть в adjacency графе как neighbors, это neighbors-связь, а не transfer
                boolean isNeighborConnection = false;
                
                // Проверяем, есть ли эта связь в adjacency графе как neighbors
                // neighbors уже получены из adjacency.get(currentStationInLine) выше
                // Если neighborStation есть в этом списке neighbors, это neighbors-связь
                if (neighbors != null) {
                    for (Station.Neighbor n : neighbors) {
                        if (n.getStation() != null && n.getStation().getId() != null 
                            && neighborStation.getId() != null 
                            && n.getStation().getId().equals(neighborStation.getId())) {
                            // Это neighbors-связь из adjacency графа
                            isNeighborConnection = true;
                            break;
                        }
                    }
                }
                
                // Определяем, какие линии обрабатывать
                // Для neighbors-связи: ВСЕГДА приоритизируем текущую линию, чтобы избежать ненужных пересадок
                // Для transfers: все доступные линии
                List<Line> linesToProcess = new ArrayList<>();
                if (isNeighborConnection && currentLine != null) {
                    // Для neighbors-связи проверяем, есть ли станция на текущей линии
                    Station neighborStationInLine = findStationInLine(neighborStation, currentLine);
                    if (neighborStationInLine != null) {
                        // Станция есть на текущей линии - ВСЕГДА используем текущую линию для neighbors-связи
                        // Это критично для избежания ненужных пересадок
                        linesToProcess.add(currentLine);
                    } else {
                        // Станция не на текущей линии - это ошибка данных, но обрабатываем все линии
                        linesToProcess.addAll(allLinesForNeighbor);
                    }
                } else {
                    // Для transfers создаем RouteStation для всех доступных линий
                    linesToProcess.addAll(allLinesForNeighbor);
                }
                
                // Если нет доступных линий, создаем RouteStation без линии
                if (linesToProcess.isEmpty()) {
                    linesToProcess.add(null);
                }
                
                // Обрабатываем каждую доступную линию
                for (Line neighborLine : linesToProcess) {
                    Station neighborStationInLine = neighborLine != null ? 
                        findStationInLine(neighborStation, neighborLine) : neighborStation;
                    if (neighborStationInLine == null) {
                        neighborStationInLine = neighborStation;
                    }
                    
                    // Ищем существующий RouteStation в distances или создаем новый
                    RouteStation neighborRouteStation = null;
                    for (RouteStation rs : distances.keySet()) {
                        if (rs.getStation() != null && rs.getStation().getId() != null 
                            && neighborStationInLine.getId() != null
                            && rs.getStation().getId().equals(neighborStationInLine.getId())
                            && ((rs.getLine() == null && neighborLine == null) 
                                || (rs.getLine() != null && neighborLine != null 
                                    && rs.getLine().getId() != null && neighborLine.getId() != null
                                    && rs.getLine().getId().equals(neighborLine.getId())))) {
                            neighborRouteStation = rs;
                            break;
                        }
                    }
                    
                    if (neighborRouteStation == null) {
                        neighborRouteStation = new RouteStation(neighborStationInLine, neighborLine);
                        distances.put(neighborRouteStation, Integer.MAX_VALUE);
                    }
                    
                    int edgeCost = edgeCostStrategy.getEdgeCost(currentStation, neighbor);
                    
                    // Для neighbors-связи НЕ должно быть смены линии - это означает пересадку
                    boolean lineChanged = false;
                    if (isNeighborConnection && currentLine != null && neighborLine != null 
                        && !currentLine.getId().equals(neighborLine.getId())) {
                        int lineChangePenalty = 10000; // Огромный штраф за смену линии при neighbors-связи
                        edgeCost += lineChangePenalty;
                        lineChanged = true;
                        Log.d("ROUTE_INFO", "    ⚠ КРИТИЧНО: neighbors-связь требует пересадки! Штраф +10000");
                    }
                    
                    // Определяем, является ли это переходом между смежными станциями (одинаковый ID, разные линии)
                    // Это НЕ neighbors-связь, а transfer между смежными станциями
                    // ВАЖНО: проверяем только если это НЕ neighbors-связь
                    boolean isAdjacentTransfer = false;
                    if (!isNeighborConnection && currentStation.getId() != null && neighborStation.getId() != null 
                        && currentStation.getId().equals(neighborStation.getId())
                        && currentLine != null && neighborLine != null
                        && !currentLine.getId().equals(neighborLine.getId())) {
                        // Это переход между смежными станциями (одинаковый ID, разные линии) через transfer
                        isAdjacentTransfer = true;
                        int adjacentTransferPenalty = 1000; // Большой штраф за переход между смежными станциями
                        edgeCost += adjacentTransferPenalty;
                    }
                    
                    // Для transfers между разными станциями добавляем штраф за смену линии
                    if (!isNeighborConnection && !isAdjacentTransfer && currentLine != null && neighborLine != null
                        && !currentLine.getId().equals(neighborLine.getId())) {
                        int transferPenalty = 10; // Штраф за переход между линиями
                        edgeCost += transferPenalty;
                    }
                    
                    int newDistance = distances.get(current) + edgeCost;
                    int neighborCurrentDistance = distances.get(neighborRouteStation);
                    
                    String connectionType = isNeighborConnection ? "NEIGHBORS" : "TRANSFER";
                    if (!isNeighborConnection) {
                        Log.d("ROUTE_INFO", String.format("    ⚠ ВНИМАНИЕ: связь определена как TRANSFER, но проверка neighbors: currentStation.neighbors=%s, neighborStation.neighbors=%s", 
                            currentStation.getNeighbors() != null ? currentStation.getNeighbors().size() + " neighbors" : "null",
                            neighborStation.getNeighbors() != null ? neighborStation.getNeighbors().size() + " neighbors" : "null"));
                    }
                    String adjacentMark = "";
                    if (isAdjacentStation) {
                        StringBuilder linesList = new StringBuilder();
                        for (int i = 0; i < allLinesForNeighbor.size(); i++) {
                            if (i > 0) linesList.append(", ");
                            String lineName = allLinesForNeighbor.get(i).getName() != null ? 
                                allLinesForNeighbor.get(i).getName() : allLinesForNeighbor.get(i).getId();
                            linesList.append(lineName);
                        }
                        adjacentMark = " [СМЕЖНАЯ: " + linesList.toString() + "]";
                    }
                    String penaltyMark = "";
                    if (isAdjacentTransfer) {
                        penaltyMark = " [ШТРАФ смежные +1000]";
                    }
                    if (lineChanged) {
                        penaltyMark += " [ШТРАФ neighbors +10000]";
                    }
                    String selectedLine = neighborLine != null ? (neighborLine.getName() != null ? neighborLine.getName() : neighborLine.getId()) : "null";
                    
                    if (newDistance < neighborCurrentDistance) {
                        distances.put(neighborRouteStation, newDistance);
                        previous.put(neighborRouteStation, current);
                        queue.add(neighborRouteStation);
                        Log.d("ROUTE_INFO", "  -> " + neighborStation.getName() + " (" + neighborStation.getId() + 
                              "), тип: " + connectionType + adjacentMark + ", линия: " + selectedLine + 
                              ", стоимость: " + edgeCost + ", расстояние: " + newDistance + " (было: " + neighborCurrentDistance + ")" + penaltyMark);
                    } else {
                        Log.d("ROUTE_INFO", "  -> " + neighborStation.getName() + " (" + neighborStation.getId() + 
                              "), тип: " + connectionType + adjacentMark + ", линия: " + selectedLine + 
                              ", стоимость: " + edgeCost + ", ПРОПУЩЕНО (новое " + newDistance + " >= текущее " + neighborCurrentDistance + ")" + penaltyMark);
                    }
                }
            }
        }

        // Восстанавливаем маршрут
        List<RouteStation> route = new ArrayList<>();
        if (endRouteStation != null) {
            for (RouteStation rs = endRouteStation; rs != null; rs = previous.get(rs)) {
                route.add(rs);
            }
            Collections.reverse(route);
            
            // Вычисляем итоговую стоимость маршрута
            int totalCost = 0;
            if (endRouteStation != null && distances.containsKey(endRouteStation)) {
                totalCost = distances.get(endRouteStation);
            }
            
            Log.d("ROUTE_INFO", "=== ФИНАЛЬНЫЙ МАРШРУТ (" + route.size() + " станций, стоимость: " + totalCost + ") ===");
            for (int i = 0; i < route.size(); i++) {
                RouteStation rs = route.get(i);
                String lineInfo = rs.getLine() != null ? 
                    (rs.getLine().getName() != null ? rs.getLine().getName() : rs.getLine().getId()) : "null";
                List<Line> allLines = findAllLinesForStation(rs.getStation());
                String adjacentInfo = "";
                if (allLines.size() > 1) {
                    StringBuilder linesList = new StringBuilder();
                    for (int j = 0; j < allLines.size(); j++) {
                        if (j > 0) linesList.append(", ");
                        String lineName = allLines.get(j).getName() != null ? 
                            allLines.get(j).getName() : allLines.get(j).getId();
                        linesList.append(lineName);
                    }
                    adjacentInfo = " [СМЕЖНАЯ: " + linesList.toString() + "]";
                }
                Log.d("ROUTE_INFO", (i + 1) + ". " + rs.getStation().getName() + " (" + rs.getStation().getId() + 
                      ") - " + lineInfo + adjacentInfo);
            }
        } else {
            Log.d("ROUTE_INFO", "=== МАРШРУТ НЕ НАЙДЕН ===");
        }
        
        return route;
    }
    
    private List<Station> convertRouteStationsToStations(List<RouteStation> routeStations) {
        if (routeStations == null) {
            return new ArrayList<>();
        }
        List<Station> stations = new ArrayList<>();
        for (RouteStation routeStation : routeStations) {
            if (routeStation != null && routeStation.getStation() != null) {
                stations.add(routeStation.getStation());
            }
        }
        return stations;
    }

    private List<Station> buildAllStationsForRouting() {
        List<Station> allStations = new ArrayList<>(stations);
        allStations.addAll(suburbanStations);
        allStations.addAll(riverTramStations);
        if (tramStations != null) {
            allStations.addAll(tramStations);
        }
        return allStations;
    }

    private Map<Station, List<Station.Neighbor>> buildRoutingAdjacency(List<Station> allStations) {
        Map<Station, List<Station.Neighbor>> adjacency = new HashMap<>();
        for (Station station : allStations) {
            adjacency.put(station, new ArrayList<>());
        }

        for (Station station : allStations) {
            if (station.getNeighbors() == null) {
                continue;
            }
            for (Station.Neighbor neighbor : station.getNeighbors()) {
                adjacency.get(station).add(new Station.Neighbor(neighbor.getStation(), neighbor.getTime()));
            }
        }

        List<Transfer> allTransfers = new ArrayList<>();
        if (transfers != null) {
            allTransfers.addAll(transfers);
        }
        if (suburbanTransfers != null) {
            allTransfers.addAll(suburbanTransfers);
        }
        if (riverTramTransfers != null) {
            allTransfers.addAll(riverTramTransfers);
        }
        if (tramTransfers != null) {
            allTransfers.addAll(tramTransfers);
        }

        for (Transfer transfer : allTransfers) {
            List<Station> transferStations = transfer.getStations();
            if (transferStations == null || transferStations.size() < 2) {
                continue;
            }
            int cost = Math.max(1, transfer.getTime());
            for (int i = 0; i < transferStations.size(); i++) {
                Station stationFrom = transferStations.get(i);
                for (int j = 0; j < transferStations.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    Station stationTo = transferStations.get(j);
                    
                    // ВАЖНО: не добавляем transfer-связь, если станции уже связаны через neighbors
                    // Это предотвращает создание переходов там, где их не должно быть
                    boolean alreadyNeighbors = false;
                    if (stationFrom.getNeighbors() != null) {
                        for (Station.Neighbor neighbor : stationFrom.getNeighbors()) {
                            if (neighbor.getStation() != null && neighbor.getStation().getId() != null
                                && stationTo.getId() != null
                                && neighbor.getStation().getId().equals(stationTo.getId())) {
                                alreadyNeighbors = true;
                                break;
                            }
                        }
                    }
                    if (alreadyNeighbors) {
                        // Станции уже связаны через neighbors - пропускаем transfer
                        continue;
                    }
                    
                    if (!adjacency.containsKey(stationFrom)) {
                        adjacency.put(stationFrom, new ArrayList<>());
                    }
                    adjacency.get(stationFrom).add(new Station.Neighbor(stationTo, cost));
                }
            }
        }

        // Для смежных станций (с одинаковым ID) объединяем neighbors
        // Это гарантирует, что все варианты смежной станции имеют одинаковые neighbors
        Map<String, List<Station>> stationsById = new HashMap<>();
        for (Station station : allStations) {
            if (station.getId() != null) {
                stationsById.computeIfAbsent(station.getId(), k -> new ArrayList<>()).add(station);
            }
        }

        for (Map.Entry<String, List<Station>> entry : stationsById.entrySet()) {
            List<Station> stationsWithSameId = entry.getValue();
            if (stationsWithSameId.size() > 1) {
                // Это смежные станции - объединяем их neighbors
                List<Station.Neighbor> allNeighbors = new ArrayList<>();
                Set<String> addedNeighborIds = new HashSet<>();
                
                // Собираем всех neighbors из всех вариантов станции
                for (Station station : stationsWithSameId) {
                    List<Station.Neighbor> stationNeighbors = adjacency.get(station);
                    if (stationNeighbors != null) {
                        for (Station.Neighbor neighbor : stationNeighbors) {
                            if (neighbor.getStation() != null && neighbor.getStation().getId() != null) {
                                String neighborId = neighbor.getStation().getId();
                                if (!addedNeighborIds.contains(neighborId)) {
                                    allNeighbors.add(neighbor);
                                    addedNeighborIds.add(neighborId);
                                }
                            }
                        }
                    }
                }
                
                // Применяем объединенные neighbors ко всем вариантам смежной станции
                for (Station station : stationsWithSameId) {
                    adjacency.put(station, new ArrayList<>(allNeighbors));
                }
            }
        }

        return adjacency;
    }

    private int getFastestEdgeCost(Station currentStation, Station.Neighbor neighbor) {
        return neighbor.getTime();
    }

    private int getFewTransfersEdgeCost(Station currentStation, Station.Neighbor neighbor) {
        Station neighborStation = neighbor.getStation();
        if (currentStation == null || neighborStation == null) {
            return neighbor.getTime();
        }
        
        // Проверяем, является ли связь neighbors-связью
        boolean isNeighborConnection = false;
        if (currentStation.getNeighbors() != null) {
            for (Station.Neighbor n : currentStation.getNeighbors()) {
                if (n.getStation() != null && n.getStation().getId() != null 
                    && neighborStation.getId() != null 
                    && n.getStation().getId().equals(neighborStation.getId())) {
                    isNeighborConnection = true;
                    break;
                }
            }
        }
        
        // Если это neighbors-связь, не добавляем штраф за переход между линиями
        if (isNeighborConnection) {
            return neighbor.getTime();
        }
        
        // Для transfers проверяем изменение цвета линии
        String currentColor = currentStation.getColor();
        String neighborColor = neighborStation.getColor();
        int baseTime = neighbor.getTime();
        if (currentColor != null && neighborColor != null && !currentColor.equals(neighborColor)) {
            int transferPenalty = 10;
            return baseTime + transferPenalty;
        }
        return baseTime;
    }

    private int countTransfersInRoute(List<RouteStation> route) {
        if (route == null || route.size() < 2) {
            return 0;
        }
        
        int transfers = 0;
        for (int i = 1; i < route.size(); i++) {
            RouteStation prevRouteStation = route.get(i - 1);
            RouteStation currentRouteStation = route.get(i);
            
            if (prevRouteStation == null || currentRouteStation == null) {
                continue;
            }
            
            Line prevLine = prevRouteStation.getLine();
            Line currentLine = currentRouteStation.getLine();
            
            if (prevLine != null && currentLine != null && !prevLine.getId().equals(currentLine.getId())) {
                transfers++;
            }
        }
        
        return transfers;
    }

    private void rebuildRouteIfPossible() {
        if (selectedStartStation != null && selectedEndStation != null) {
            if (metroMapView != null) {
                metroMapView.clearRoute();
            }
            List<RouteStation> fastestRoute = findOptimalRoute(selectedStartStation, selectedEndStation);
            List<RouteStation> fewTransfersRoute = findRouteWithFewTransfers(selectedStartStation, selectedEndStation);
            if (fastestRoute != null && !fastestRoute.isEmpty()) {
                if (metroMapView != null) {
                    metroMapView.setRouteFromRouteStations(fastestRoute);
                }
                showRouteInfo(fastestRoute, fewTransfersRoute);
            } else {
                if (metroMapView != null) {
                    metroMapView.clearRoute();
                }
            }
        } else {
            if (metroMapView != null) {
                metroMapView.clearRoute();
            }
        }
    }

    public void clearRouteInputs() {
        if (metroMapView != null) {
            metroMapView.clearRoute();
        }
        startStationEditText.setText("");
        endStationEditText.setText("");
        if (selectedStartStation != null) {
            if (selectedStations != null) {
                selectedStations.remove(selectedStartStation);
            }
        }
        if (selectedEndStation != null) {
            if (selectedStations != null) {
                selectedStations.remove(selectedEndStation);
            }
        }
        selectedStartStation = null;
        selectedEndStation = null;
        if (selectedStations != null) {
            selectedStations.clear();
        }
        if (metroMapView != null) {
            metroMapView.clearSelectedStations();
        }
    }

    private void clearRoute() {
        metroMapView.clearRoute();
        clearFrameLayout();
    }

    private void showRouteInfo(List<RouteStation> route) {
        clearFrameLayout();
        List<Station> stationRoute = convertRouteStationsToStations(route);
        Map<Station, Line> routeLineMap = createRouteLineMap(route);
        RouteInfoFragment routeInfoFragment = RouteInfoFragment.newInstance(stationRoute, metroMapView, this);
        routeInfoFragment.setRouteLineMap(routeLineMap);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, routeInfoFragment)
                .commit();
    }
    
    private Map<Station, Line> createRouteLineMap(List<RouteStation> routeStations) {
        Map<Station, Line> routeLineMap = new HashMap<>();
        if (routeStations != null) {
            for (RouteStation routeStation : routeStations) {
                if (routeStation != null && routeStation.getStation() != null && routeStation.getLine() != null) {
                    routeLineMap.put(routeStation.getStation(), routeStation.getLine());
                }
            }
        }
        return routeLineMap;
    }

    private void showRouteInfo(List<RouteStation> fastestRoute, List<RouteStation> fewTransfersRoute) {
        clearFrameLayout();
        
        if (fastestRoute == null || fewTransfersRoute == null || 
            fastestRoute.isEmpty() || fewTransfersRoute.isEmpty()) {
            List<RouteStation> routeToUse = fastestRoute != null && !fastestRoute.isEmpty() ? fastestRoute : fewTransfersRoute;
            List<Station> stationRoute = convertRouteStationsToStations(routeToUse);
            Map<Station, Line> routeLineMap = createRouteLineMap(routeToUse);
            RouteInfoFragment routeInfoFragment = RouteInfoFragment.newInstance(
                    stationRoute,
                    metroMapView,
                    this
            );
            routeInfoFragment.setRouteLineMap(routeLineMap);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, routeInfoFragment)
                    .commit();
            LinearLayout routePagerDots = findViewById(R.id.routePagerDots);
            if (routePagerDots != null) {
                routePagerDots.setVisibility(View.GONE);
            }
            return;
        }
        
        boolean routesAreEqual = fastestRoute.size() == fewTransfersRoute.size();
        if (routesAreEqual) {
            for (int i = 0; i < fastestRoute.size(); i++) {
                if (!fastestRoute.get(i).getStation().getId().equals(fewTransfersRoute.get(i).getStation().getId())) {
                    routesAreEqual = false;
                    break;
                }
            }
        }
        
        if (routesAreEqual) {
            List<RouteStation> routeToUse = fastestRoute != null && !fastestRoute.isEmpty() ? fastestRoute : fewTransfersRoute;
            List<Station> stationRoute = convertRouteStationsToStations(routeToUse);
            Map<Station, Line> routeLineMap = createRouteLineMap(routeToUse);
            RouteInfoFragment routeInfoFragment = RouteInfoFragment.newInstance(
                    stationRoute,
                    metroMapView,
                    this
            );
            routeInfoFragment.setRouteLineMap(routeLineMap);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, routeInfoFragment)
                    .commit();
            LinearLayout routePagerDots = findViewById(R.id.routePagerDots);
            if (routePagerDots != null) {
                routePagerDots.setVisibility(View.GONE);
            }
            return;
        }
        
        FrameLayout frameLayout = findViewById(R.id.frameLayout);
        if (frameLayout == null) return;
        
        frameLayout.removeAllViews();
        
        ViewPager2 routePager = new ViewPager2(this);
        routePager.setId(View.generateViewId());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        routePager.setLayoutParams(params);
        frameLayout.addView(routePager);
        
        RoutePagerAdapter routePagerAdapter = new RoutePagerAdapter(
                this,
                fastestRoute,
                fewTransfersRoute,
                metroMapView,
                this
        );
        currentRoutePagerAdapter = routePagerAdapter;
        routePager.setAdapter(routePagerAdapter);
        
        addRoutePagerDots(routePager, routePagerAdapter);
        
        if (metroMapView != null && !fastestRoute.isEmpty()) {
            metroMapView.setRouteFromRouteStations(fastestRoute);
        }
    }

    public Station findStationByNameAndAPITariff(String stationName) {
        for (Line line : allLines) {
            if (line.getTariff() instanceof APITariff) {
                for (Station station : line.getStations()) {
                    if (station.getName().equals(stationName)) {
                        return station;
                    }
                }
            }
        }
        return null;
    }

    private void showStationsList(List<Station> stations) {
        stationsRecyclerView.setVisibility(View.VISIBLE);
        stationsAdapter.setStations(stations);
    }

    private void hideStationsList() {
        stationsRecyclerView.setVisibility(View.GONE);
    }

    private void filterStations(String query) {
        List<Station> filteredStations = new ArrayList<>();
        for (Station station : stations) {
            if (station.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredStations.add(station);
            }
        }
        stationsAdapter.setStations(filteredStations);
    }

    @Override
    public void onStationSelected(Station station) {
        if (startStationEditText.hasFocus()) {
            setStationInfo(startStationEditText, station);
            onSetStart(station, null, true);
        } else if (endStationEditText.hasFocus()) {
            setStationInfo(endStationEditText, station);
            onSetEnd(station, null, true);
        }
        hideStationsList();
    }

    public List<Transfer> getAllTransfers() {
        return transfers;
    }
}