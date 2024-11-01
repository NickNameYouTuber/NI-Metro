package com.nicorp.nimetro;

import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends AppCompatActivity implements MetroMapView.OnStationClickListener, StationInfoDialogFragment.OnStationInfoListener, StationsAdapter.OnStationClickListener {

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

        metroMapView = findViewById(R.id.metroMapView);
        startStationLayout = findViewById(R.id.startStationLayout);
        endStationLayout = findViewById(R.id.endStationLayout);
        startStationEditText = findViewById(R.id.startStationEditText);
        endStationEditText = findViewById(R.id.endStationEditText);
        stationsRecyclerView = findViewById(R.id.stationsRecyclerView);

        stations = new ArrayList<>();
        lines = new ArrayList<>();
        selectedStations = new ArrayList<>();

        loadMetroData();
        metroMapView.setData(lines, stations);
        metroMapView.setOnStationClickListener(this);

        stationsAdapter = new StationsAdapter(stations, this);
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

    private void loadMetroData() {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());
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

            // Add transfers between different lines
            JSONArray transfersArray = jsonObject.getJSONArray("transfers");
            for (int i = 0; i < transfersArray.length(); i++) {
                JSONObject transferObject = transfersArray.getJSONObject(i);
                int station1Id = transferObject.getInt("station1");
                int station2Id = transferObject.getInt("station2");

                Station station1 = findStationById(station1Id);
                Station station2 = findStationById(station2Id);

                if (station1 != null && station2 != null) {
                    station1.addNeighbor(new Station.Neighbor(station2, 3)); // Assuming transfer time is 3 minutes
                    station2.addNeighbor(new Station.Neighbor(station1, 3));
                }
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

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getResources().openRawResource(R.raw.metro_data);
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
        StationInfoDialogFragment dialogFragment = StationInfoDialogFragment.newInstance(station);
        dialogFragment.setOnStationInfoListener(this);
        dialogFragment.show(getSupportFragmentManager(), "station_info");
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

    private void showRouteInfo(List<Station> route) {
        RouteInfoDialogFragment dialogFragment = RouteInfoDialogFragment.newInstance(route);
        dialogFragment.show(getSupportFragmentManager(), "route_info");
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