package com.nicorp.nimetro.presentation.activities;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
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
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.APITariff;
import com.nicorp.nimetro.domain.entities.FlatRateTariff;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Tariff;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.domain.entities.TransferRoute;
import com.nicorp.nimetro.domain.entities.ZoneBasedTariff;
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

    private TextInputLayout startStationLayout;
    private TextInputLayout endStationLayout;
    private TextInputEditText startStationEditText;
    private TextInputEditText endStationEditText;
    private RecyclerView stationsRecyclerView;
    private StationsAdapter stationsAdapter;

    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;
    private static final long LOCATION_UPDATE_INTERVAL = 30000; // 30 секунд

    public static boolean isMetroMap = true; // Флаг для определения текущей карты
    public static boolean isSuburbanMap = false;
    public static boolean isRiverTramMap = false;


    private boolean isTrackingServiceRunning = false;

    public void startStationTrackingService(Station station) {
        if (isTrackingServiceRunning) return; // Уже запущен

        Intent serviceIntent = new Intent(this, StationTrackingService.class);
        serviceIntent.putExtra("currentStation", station);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isTrackingServiceRunning = true;
    }

    public void stopStationTrackingService() {
        if (!isTrackingServiceRunning) return; // Уже остановлен

        Intent serviceIntent = new Intent(this, StationTrackingService.class);
        stopService(serviceIntent);
        isTrackingServiceRunning = false;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocation();

//        // Инициализация Handler и Runnable для периодического обновления местоположения
//        locationUpdateHandler = new Handler();
//        locationUpdateRunnable = new Runnable() {
//            @Override
//            public void run() {
//                requestLocation();
//                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
//            }
//        };
//
//        startLocationUpdates();

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
        switchMapButton.setOnClickListener(v -> {
            if (isMetroMap) {
                isMetroMap = false;
                isSuburbanMap = false;
                isRiverTramMap = true;
                switchMapButton.setImageResource(R.drawable.river_tram_icon);
            } else if (isSuburbanMap) {
                isMetroMap = true;
                isSuburbanMap = false;
                isRiverTramMap = false;
                switchMapButton.setImageResource(R.drawable.metro_map_icon);
            } else {
                isMetroMap = false;
                isSuburbanMap = true;
                isRiverTramMap = false;
                switchMapButton.setImageResource(R.drawable.suburban_map_icon);
            }
            updateMapData();
        });

        ConstraintLayout settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Settings button clicked");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
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

        loadMetroData(selectedMapFileName);

        stationsAdapter = new StationsAdapter(new ArrayList<Station>(), this);
        stationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stationsRecyclerView.setAdapter(stationsAdapter);

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


    private void startLocationUpdates() {
        locationUpdateHandler.post(locationUpdateRunnable);
    }

    private void stopLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Остановка обновления местоположения при уничтожении активности
        stopLocationUpdates();
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

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject metroMapData = jsonObject.optJSONObject("metro_map");
            JSONObject suburbanMapData = jsonObject.optJSONObject("suburban_map");
            JSONObject riverTramMapData = jsonObject.optJSONObject("rivertram_map");

            // Загружаем данные для метро
            if (metroMapData != null) {
                loadMapData(metroMapData, lines, stations, transfers, rivers, mapObjects);
            }

            // Загружаем данные для электричек, если они есть
            if (suburbanMapData != null) {
                loadMapData(suburbanMapData, suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects);
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
            if (metroMapData != null) {
                addNeighbors(metroMapData, allStations);
            }
            if (suburbanMapData != null) {
                addNeighbors(suburbanMapData, allStations);
            }
            if (riverTramMapData != null) {
                addNeighbors(riverTramMapData, allStations);
            }

            allLines = new ArrayList<>(lines);
            if (suburbanMapData != null) {
                allLines.addAll(suburbanLines);
            }
            if (riverTramMapData != null) {
                allLines.addAll(riverTramLines);
            }

            updateMapData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateMapData() {
        if (isMetroMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    true, false, false
            );
        } else if (isSuburbanMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    false, true, false
            );
        } else if (isRiverTramMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    false, false, true
            );
        }
    }

    private void loadMapData(JSONObject mapData, List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) throws JSONException {
        if (lines == null) lines = new ArrayList<>();
        if (stations == null) stations = new ArrayList<>();
        if (transfers == null) transfers = new ArrayList<>();
        if (rivers == null) rivers = new ArrayList<>();
        if (mapObjects == null) mapObjects = new ArrayList<>();
        JSONArray linesArray = mapData.getJSONArray("lines");

        for (int i = 0; i < linesArray.length(); i++) {
            JSONObject lineObject = linesArray.getJSONObject(i);
            boolean isCircle = lineObject.optBoolean("isCircle", false);
            String lineType = lineObject.optString("lineType", "single");
            String displayNumber = lineObject.optString("displayNumber", null);
            String displayShape = lineObject.optString("displayShape", null);
            Tariff tariff = createTariff(lineObject.optJSONObject("tariff"));
            Line line = new Line(lineObject.getString("id"), lineObject.getString("name"), lineObject.getString("color"), isCircle, lineType, tariff, displayNumber, displayShape);
            JSONArray stationsArray = lineObject.getJSONArray("stations");
            for (int j = 0; j < stationsArray.length(); j++) {
                JSONObject stationObject = stationsArray.getJSONObject(j);
                String schedule = stationObject.optString("schedule", "5:30 - 0:00");
                int escalators = stationObject.optInt("escalators", 0);
                int elevators = stationObject.optInt("elevators", 0);
                String[] exits = toStringArray(stationObject.optJSONArray("exits"));
                int textPosition = stationObject.optInt("textPosition", 0);
                String ESP = stationObject.optString("ESP", null); // Добавляем поле ESP
// Загружаем широту и долготу
                double latitude = stationObject.optDouble("latitude", 0.0);
                double longitude = stationObject.optDouble("longitude", 0.0);
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

                // Устанавливаем широту и долготу
                if (latitude != 0.0 && longitude != 0.0 && !Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                    station.setLatitude(latitude);
                    station.setLongitude(longitude);
                }

                stations.add(station);
                line.getStations().add(station);
            }
            lines.add(line);
        }

        JSONArray transfersArray = mapData.getJSONArray("transfers");
        for (int i = 0; i < transfersArray.length(); i++) {
            JSONObject transferObject = transfersArray.getJSONObject(i);
            JSONArray stationsArray = transferObject.getJSONArray("stations");
            List<Station> transferStations = new ArrayList<>();
            for (int j = 0; j < stationsArray.length(); j++) {
                String stationId = stationsArray.getString(j);
                Station station = findStationById(stationId, stations);
                if (station != null) {
                    transferStations.add(station);
                }
            }
            int time = transferObject.optInt("time", 3);
            String type = transferObject.optString("type", "regular");
            String transferMap = transferObject.optString("transfer_map", null);

            // Парсинг transfer_routes
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
                        for (int l = 0; l < wayArray.length(); l++) {
                            way.add(wayArray.getString(l));
                        }
                    }
                    transferRoutes.add(new TransferRoute(routeTransferMap, from, to, way, prev, null));
                }
            }

            // Создание объекта Transfer с учетом transferRoutes
            transfers.add(new Transfer(transferStations, time, type, transferMap, transferRoutes));
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
                    JSONArray neighborsArray = stationObject.getJSONArray("neighbors");
                    for (int k = 0; k < neighborsArray.length(); k++) {
                        JSONArray neighborArray = neighborsArray.getJSONArray(k);
                        String neighborId = neighborArray.getString(0);
                        int time = neighborArray.getInt(1);
                        Station neighborStation = findStationById(neighborId, allStations);
                        if (neighborStation != null) {
                            station.addNeighbor(new Station.Neighbor(neighborStation, time));
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
                    break;
                }
            }
        }

        // Очистка предыдущего фрагмента
        clearFrameLayout();

        // Создание и установка адаптера для ViewPager2
        StationPagerAdapter pagerAdapter = new StationPagerAdapter(
                this, station, curline, prevStation, nextStation,
                transfers, lines, suburbanLines, this
        );

        ViewPager2 stationPager = findViewById(R.id.stationPager);
        stationPager.setAdapter(pagerAdapter);
        stationPager.setVisibility(View.VISIBLE); // Показываем ViewPager2
    }

    // Метод для получения активных линий в зависимости от текущей карты
    private List<Line> getActiveLines() {
        if (isMetroMap) {
            return lines;
        } else if (isSuburbanMap) {
            return suburbanLines;
        } else if (isRiverTramMap) {
            return riverTramLines;
        }
        return Collections.emptyList(); // Возвращаем пустой список, если ни одна карта не активна
    }

    @Override
    public void onDismiss() {
        ViewPager2 stationPager = findViewById(R.id.stationPager);
        stationPager.setVisibility(View.GONE); // Скрываем ViewPager2
    }

    private void clearFrameLayout() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.frameLayout);
        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    @Override
    public void onSetStart(Station station, boolean fromStationInfoFragment) {
        if (selectedStartStation != null) {
            selectedStations.remove(selectedStartStation);
        }
        selectedStartStation = station;
        selectedStations.add(station);
        metroMapView.setSelectedStations(selectedStations);
        startStationEditText.setText(station.getName());

        if (fromStationInfoFragment) {
            hideStationsList();
        }
    }

    @Override
    public void onSetEnd(Station station, boolean fromStationInfoFragment) {
        if (selectedEndStation != null) {
            selectedStations.remove(selectedEndStation);
        }
        selectedEndStation = station;
        selectedStations.add(station);
        metroMapView.setSelectedStations(selectedStations);
        endStationEditText.setText(station.getName());

        if (selectedStartStation != null) {
            List<Station> route = findOptimalRoute(selectedStartStation, selectedEndStation);
            metroMapView.setRoute(route);
            showRouteInfo(route);
        }

        if (fromStationInfoFragment) {
            hideStationsList();
        }
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
                                onSetStart(nearestStation, false);
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

    private List<Station> findOptimalRoute(Station start, Station end) {
        Log.d("MainActivity", "Finding optimal route from " + start.getName() + " to " + end.getName());
        Map<Station, Station> previous = new HashMap<>();
        Map<Station, Integer> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        List<Station> allStations = new ArrayList<>(stations);
        allStations.addAll(suburbanStations);
        allStations.addAll(riverTramStations);

        for (Station station : allStations) {
            distances.put(station, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            if (current.getId().equals(end.getId())) {
                break;
            }

            for (Station.Neighbor neighbor : current.getNeighbors()) {
                Station neighborStation = neighbor.getStation(); // Use getStation() instead of getStationId()
                int distance = distances.get(current) + neighbor.getTime();
                if (distance < distances.get(neighborStation)) {
                    distances.put(neighborStation, distance);
                    previous.put(neighborStation, current);
                    queue.add(neighborStation);
                }
            }
        }

        List<Station> route = new ArrayList<>();
        for (Station station = end; station != null; station = previous.get(station)) {
            route.add(station);
        }
        Collections.reverse(route);
        return route;
    }

    public void clearRouteInputs() {
        startStationEditText.setText("");
        endStationEditText.setText("");
    }

    private void showRouteInfo(List<Station> route) {
        clearFrameLayout();
        RouteInfoFragment routeInfoFragment = RouteInfoFragment.newInstance(route, metroMapView, this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, routeInfoFragment)
                .commit();
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
            startStationEditText.setText(station.getName());
            onSetStart(station, true);
        } else if (endStationEditText.hasFocus()) {
            endStationEditText.setText(station.getName());
            onSetEnd(station, true);
        }
        hideStationsList();
    }

    public List<Transfer> getAllTransfers() {
        return transfers;
    }
}