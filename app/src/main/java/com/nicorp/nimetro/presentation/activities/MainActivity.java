package com.nicorp.nimetro.presentation.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

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
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;
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
    private Station selectedStartStation;
    private Station selectedEndStation;
    private List<Station> selectedStations;

    private TextInputLayout startStationLayout;
    private TextInputLayout endStationLayout;
    private TextInputEditText startStationEditText;
    private TextInputEditText endStationEditText;
    private RecyclerView stationsRecyclerView;
    private StationsAdapter stationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Кнопка настроек
        ConstraintLayout settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Settings button clicked");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "metromap_1.json");
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        metroMapView = findViewById(R.id.metroMapView);
        startStationEditText = findViewById(R.id.startStationEditText);
        endStationEditText = findViewById(R.id.endStationEditText);
        stationsRecyclerView = findViewById(R.id.stationsRecyclerView);

        stations = new ArrayList<>();
        lines = new ArrayList<>();
        selectedStations = new ArrayList<>();

        loadMetroData(selectedMapFileName);
        metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
        metroMapView.setOnStationClickListener(this);

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

    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects; // Добавлен список объектов

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONArray linesArray = jsonObject.getJSONArray("lines");

            // First, load all stations and lines
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject lineObject = linesArray.getJSONObject(i);
                boolean isCircle = lineObject.optBoolean("isCircle", false);
                Line line = new Line(lineObject.getInt("id"), lineObject.getString("name"), lineObject.getString("color"), isCircle);
                JSONArray stationsArray = lineObject.getJSONArray("stations");
                for (int j = 0; j < stationsArray.length(); j++) {
                    JSONObject stationObject = stationsArray.getJSONObject(j);
                    String schedule = stationObject.optString("schedule", "5:30 - 0:00");
                    int escalators = stationObject.optInt("escalators", 0);
                    int elevators = stationObject.optInt("elevators", 0);
                    String[] exits = toStringArray(stationObject.optJSONArray("exits"));
                    int textPosition = stationObject.optInt("textPosition", 0);

                    Facilities facilities = new Facilities(schedule, escalators, elevators, exits);
                    Station station = new Station(
                            stationObject.getInt("id"),
                            stationObject.getString("name"),
                            stationObject.getInt("x"),
                            stationObject.getInt("y"),
                            line.getColor(),
                            facilities,
                            textPosition
                    );
                    stations.add(station);
                    line.getStations().add(station);
                }
                lines.add(line);
            }

            // Now, add neighbors and transfers
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject lineObject = linesArray.getJSONObject(i);
                JSONArray stationsArray = lineObject.getJSONArray("stations");
                for (int j = 0; j < stationsArray.length(); j++) {
                    JSONObject stationObject = stationsArray.getJSONObject(j);
                    Station station = findStationById(stationObject.getInt("id"));
                    if (station != null) {
                        JSONArray neighborsArray = stationObject.getJSONArray("neighbors");
                        for (int k = 0; k < neighborsArray.length(); k++) {
                            JSONArray neighborArray = neighborsArray.getJSONArray(k);
                            int neighborId = neighborArray.getInt(0);
                            int time = neighborArray.getInt(1);
                            Station neighborStation = findStationById(neighborId);
                            if (neighborStation != null) {
                                station.addNeighbor(new Station.Neighbor(neighborStation, time));
                            }
                        }
                    }
                }
            }

            // Load transfers between different lines
            JSONArray transfersArray = jsonObject.getJSONArray("transfers");
            transfers = new ArrayList<>();
            for (int i = 0; i < transfersArray.length(); i++) {
                JSONObject transferObject = transfersArray.getJSONObject(i);
                JSONArray stationsArray = transferObject.getJSONArray("stations");
                List<Station> transferStations = new ArrayList<>();
                for (int j = 0; j < stationsArray.length(); j++) {
                    int stationId = stationsArray.getInt(j);
                    Station station = findStationById(stationId);
                    if (station != null) {
                        transferStations.add(station);
                    }
                }
                int time = transferObject.optInt("time", 3); // Assuming transfer time is 3 minutes
                String type = transferObject.optString("type", "regular");
                transfers.add(new Transfer(transferStations, time, type));
            }

            // Load rivers
            JSONArray riversArray = jsonObject.getJSONArray("rivers");
            rivers = new ArrayList<>();
            for (int i = 0; i < riversArray.length(); i++) {
                JSONObject riverObject = riversArray.getJSONObject(i);
                JSONArray pointsArray = riverObject.getJSONArray("points");
                List<Point> riverPoints = new ArrayList<>();
                for (int j = 0; j < pointsArray.length(); j++) {
                    JSONObject pointObject = pointsArray.getJSONObject(j);
                    Point point = new Point(pointObject.getInt("x"), pointObject.getInt("y"));
                    riverPoints.add(point);
                }
                int width = riverObject.optInt("width", 10); // Default width is 10
                rivers.add(new River(riverPoints, width));
            }

            // Load intermediate points
            JSONArray intermediatePointsArray = jsonObject.getJSONArray("intermediatePoints");
            for (int i = 0; i < intermediatePointsArray.length(); i++) {
                JSONObject intermediatePointObject = intermediatePointsArray.getJSONObject(i);
                JSONArray neighborsIdArray = intermediatePointObject.getJSONArray("neighborsId");
                int station1Id = neighborsIdArray.getInt(0);
                int station2Id = neighborsIdArray.getInt(1);

                Station station1 = findStationById(station1Id);
                Station station2 = findStationById(station2Id);

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

            // Load map objects
            JSONArray objectsArray = jsonObject.getJSONArray("objects");
            mapObjects = new ArrayList<>();
            for (int i = 0; i < objectsArray.length(); i++) {
                JSONObject objectObject = objectsArray.getJSONObject(i);
                String name = objectObject.getString("name");
                String displayName = objectObject.getString("displayName");
                String type = objectObject.getString("type");
                JSONObject positionObject = objectObject.getJSONObject("position");
                Point position = new Point(positionObject.getInt("x"), positionObject.getInt("y"));
                mapObjects.add(new MapObject(name, type, position, displayName));
            }
        } catch (JSONException e) {
            e.printStackTrace();
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

    private Station findStationById(int id) {
        for (Station station : stations) {
            if (station.getId() == id) {
                return station;
            }
        }
        return null;
    }

    @Override
    public void onStationClick(Station station) {
        Station prevStation = null;
        Station nextStation = null;
        Line curline = null;

        // Найдите предыдущую и следующую станции на той же линии
        for (Line line : lines) {
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

        // Очистите FrameLayout перед добавлением нового фрагмента
        clearFrameLayout();

        StationInfoFragment fragment = StationInfoFragment.newInstance(curline, station, prevStation, nextStation, transfers, lines);
        fragment.setOnStationInfoListener(this);

        // Добавьте новый фрагмент в FrameLayout
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
    public void onSetStart(Station station) {
        if (selectedStartStation != null) {
            selectedStations.remove(selectedStartStation);
        }
        selectedStartStation = station;
        selectedStations.add(station);
        metroMapView.setSelectedStations(selectedStations);
        startStationEditText.setText(station.getName());
    }

    @Override
    public void onSetEnd(Station station) {
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
    }

    private List<Station> findOptimalRoute(Station start, Station end) {
        Map<Station, Station> previous = new HashMap<>();
        Map<Station, Integer> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (Station station : stations) {
            distances.put(station, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            if (current == end) {
                break;
            }

            for (Station.Neighbor neighbor : current.getNeighbors()) {
                int distance = distances.get(current) + neighbor.getTime();
//                if (neighbor.getStation().getLineId() != current.getLineId()) {
//                    // Если станция на другой линии, добавляем время перехода
//                    distance += 3; // Предположим, что время перехода 3 минуты
//                }
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

//    private void showRouteInfo(List<Station> route) {
//        BottomFragment bottomFragment = BottomFragment.newInstance(route);
//        bottomFragment.show(getSupportFragmentManager(), "bottom_fragment");
////        BottomFragment bottomFragment = new BottomFragment();
////        bottomFragment.show(getSupportFragmentManager(), "route_info");
//    }

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
            onSetStart(station);
        } else if (endStationEditText.hasFocus()) {
            endStationEditText.setText(station.getName());
            onSetEnd(station);
        }
        hideStationsList();
    }
}