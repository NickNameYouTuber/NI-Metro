package com.nicorp.nimetro.presentation.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.nicorp.nimetro.domain.entities.ZoneBasedTariff;
import com.nicorp.nimetro.presentation.views.MetroMapView;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.presentation.fragments.RouteInfoFragment;
import com.nicorp.nimetro.presentation.fragments.StationInfoFragment;
import com.nicorp.nimetro.presentation.adapters.StationsAdapter;
import com.nicorp.nimetro.domain.entities.Facilities;

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
    private List<Station> grayedStations;
    private List<Line> grayedLines;
    private Station selectedStartStation;
    private Station selectedEndStation;
    private List<Station> selectedStations;

    private TextInputLayout startStationLayout;
    private TextInputLayout endStationLayout;
    private TextInputEditText startStationEditText;
    private TextInputEditText endStationEditText;
    private RecyclerView stationsRecyclerView;
    private StationsAdapter stationsAdapter;

    private boolean isMetroMap = true; // Флаг для определения текущей карты

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

        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        ImageView switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> {
            isMetroMap = !isMetroMap;
            Log.d("MainActivity", "Switched map to " + (isMetroMap ? "metro" : "suburban"));
            if (isMetroMap) {
                Log.d("MainActivity", "SwTest");
                switchMapButton.setImageResource(R.drawable.metro_map_icon);
                Log.d("MainActivity", "SwTest");
            } else {
                Log.d("MainActivity", "SwTest");
                switchMapButton.setImageResource(R.drawable.suburban_map_icon);
                Log.d("MainActivity", "SwTest");
            }
            Log.d("MainActivity", "Switched map to " + (isMetroMap ? "metro" : "suburban"));
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

        stations = new ArrayList<>();
        lines = new ArrayList<>();
        grayedStations = new ArrayList<>();
        grayedLines = new ArrayList<>();
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

    private List<Transfer> transfers = new ArrayList<>();
    private List<River> rivers;
    private List<MapObject> mapObjects;
    private List<Transfer> grayedTransfers;
    private List<River> grayedRivers;
    private List<MapObject> grayedMapObjects;
    private List<Line> allLines;

    public List<Line> getAllLines() {
        return allLines;
    }

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject metroMapData = jsonObject.optJSONObject("metro_map");
            JSONObject suburbanMapData = jsonObject.optJSONObject("suburban_map");

            // Загружаем данные для метро
            if (metroMapData != null) {
                loadMapData(metroMapData, lines, stations, transfers, rivers, mapObjects);
            }

            // Загружаем данные для электричек, если они есть
            if (suburbanMapData != null) {
                loadMapData(suburbanMapData, grayedLines, grayedStations, grayedTransfers, grayedRivers, grayedMapObjects);
            }

            // Объединяем станции и добавляем соседей
            List<Station> allStations = new ArrayList<>(stations);
            if (suburbanMapData != null) {
                allStations.addAll(grayedStations);
            }
            if (metroMapData != null) {
                addNeighbors(metroMapData, allStations);
            }
            if (suburbanMapData != null) {
                addNeighbors(suburbanMapData, allStations);
            }

            allLines = new ArrayList<>(lines);
            if (suburbanMapData != null) {
                allLines.addAll(grayedLines);
            }

            updateMapData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateMapData() {
        Log.d("MetroMapView_MainActivity", "isMetroMap " + isMetroMap);
        if (isMetroMap) {
            Log.d("MetroMapView_MainActivity", "Station 1: " + stations.get(0).getName());
            Log.d("MetroMapView_MainActivity", "Gray Station 1: " + grayedStations.get(0).getName());
            Log.d("MetroMapView_MainActivity", "Transfer 1: " + transfers.get(0).getStations().get(0).getName());
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, grayedLines, grayedStations);
        } else {
            Log.d("MetroMapView_MainActivity", "Station 1: " + stations.get(0).getName());
            Log.d("MetroMapView_MainActivity", "Gray Station 1: " + grayedStations.get(0).getName());
            metroMapView.setData(grayedLines, grayedStations, grayedTransfers, grayedRivers, grayedMapObjects, lines, stations);
        }
        metroMapView.setActiveMap(isMetroMap);
    }

    private void loadMapData(JSONObject mapData, List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) throws JSONException {
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
                Log.d("MetroMapView_MainActivity", "Transfer Station ID: " + stationId);
                Station station = findStationById(stationId, stations);
                if (station != null) {
                    Log.d("MetroMapView_MainActivity", "Transfer Station: " + station.getName());
                    transferStations.add(station);
                }
            }
            int time = transferObject.optInt("time", 3);
            String type = transferObject.optString("type", "regular");
            Log.d("MetroMapView_MainActivity", "Transfer Type: " + type);
            Log.d("MetroMapView_MainActivity", "Transfer Time: " + time);
            transfers.add(new Transfer(transferStations, time, type));
        }

        JSONArray riversArray = mapData.getJSONArray("rivers");
        for (int i = 0; i < riversArray.length(); i++) {
            JSONObject riverObject = riversArray.getJSONObject(i);
            JSONArray pointsArray = riverObject.getJSONArray("points");
            List<Point> riverPoints = new ArrayList<>();
            for (int j = 0; j < pointsArray.length(); j++) {
                JSONObject pointObject = pointsArray.getJSONObject(j);
                Point point = new Point(pointObject.getInt("x"), pointObject.getInt("y"));
                riverPoints.add(point);
            }
            int width = riverObject.optInt("width", 10);
            rivers.add(new River(riverPoints, width));
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

        for (Line line : lines) {
            Log.d("MainActivity", "Line: " + line.getName());
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
                    break;
                }
            }
        }
        for (Line line : grayedLines) {
            Log.d("MainActivity", "Line: " + line.getName());
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
                    break;
                }
            }
        }

        clearFrameLayout();

        Log.d("MainActivity", "station fragment inputs " + curline.getName() + ", " + station.getName());
        StationInfoFragment fragment = StationInfoFragment.newInstance(curline, station, prevStation, nextStation, transfers, lines, grayedLines);
        fragment.setOnStationInfoListener(this);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, fragment)
                .commit();
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

    private List<Station> findOptimalRoute(Station start, Station end) {
        Log.d("MainActivity", "Finding optimal route from " + start.getName() + " to " + end.getName());
        Map<Station, Station> previous = new HashMap<>();
        Map<Station, Integer> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        List<Station> allStations = new ArrayList<>(stations);
        allStations.addAll(grayedStations);

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
                int distance = distances.get(current) + neighbor.getTime();
                if (distance < distances.get(neighbor.getStation())) {
                    distances.put(neighbor.getStation(), distance);
                    previous.put(neighbor.getStation(), current);
                    queue.add(neighbor.getStation());
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
}