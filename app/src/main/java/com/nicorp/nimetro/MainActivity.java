package com.nicorp.nimetro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends AppCompatActivity implements MetroMapView.OnStationClickListener, StationInfoDialogFragment.OnStationInfoListener {

    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private Station selectedStartStation;
    private Station selectedEndStation;
    private List<Station> selectedStations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        metroMapView = findViewById(R.id.metroMapView);
        stations = new ArrayList<>();
        lines = new ArrayList<>();
        selectedStations = new ArrayList<>();

        loadMetroData();
        metroMapView.setData(lines, stations);
        metroMapView.setOnStationClickListener(this);
    }

    private void loadMetroData() {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());
            JSONArray linesArray = jsonObject.getJSONArray("lines");
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject lineObject = linesArray.getJSONObject(i);
                Line line = new Line(lineObject.getInt("id"), lineObject.getString("name"), lineObject.getString("color"), lineObject.getBoolean("isCircle"));
                JSONArray stationsArray = lineObject.getJSONArray("stations");
                for (int j = 0; j < stationsArray.length(); j++) {
                    JSONObject stationObject = stationsArray.getJSONObject(j);
                    String schedule = stationObject.optString("schedule", "5:30 - 0:00");
                    int escalators = stationObject.optInt("escalators", 0);
                    int elevators = stationObject.optInt("elevators", 0);
                    String[] exits = toStringArray(stationObject.optJSONArray("exits"));

                    Facilities facilities = new Facilities(schedule, escalators, elevators, exits);
                    Station station = new Station(
                            stationObject.getInt("id"),
                            stationObject.getString("name"),
                            stationObject.getInt("x"),
                            stationObject.getInt("y"),
                            line.getColor(),
                            facilities
                    );
                    stations.add(station);
                    line.getStations().add(station);
                }
                lines.add(line);
            }

            // Add neighbors within the same line
            for (Line line : lines) {
                for (int i = 0; i < line.getStations().size() - 1; i++) {
                    Station station1 = line.getStations().get(i);
                    Station station2 = line.getStations().get(i + 1);
                    station1.addNeighbor(station2);
                    station2.addNeighbor(station1);
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
                    station1.addNeighbor(station2);
                    station2.addNeighbor(station1);
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
            selectedStations.remove(selectedStartStation); // Удаляем предыдущую выбранную станцию
        }
        selectedStartStation = station;
        selectedStations.add(station); // Добавляем новую выбранную станцию
        metroMapView.setSelectedStations(selectedStations); // Обновляем выбранные станции на карте
    }

    @Override
    public void onSetEnd(Station station) {
        if (selectedEndStation != null) {
            selectedStations.remove(selectedEndStation); // Удаляем предыдущую выбранную станцию
        }
        selectedEndStation = station;
        selectedStations.add(station); // Добавляем новую выбранную станцию
        metroMapView.setSelectedStations(selectedStations); // Обновляем выбранные станции на карте

        if (selectedStartStation != null) {
            List<Station> route = findOptimalRoute(selectedStartStation, selectedEndStation);
            metroMapView.setRoute(route);
            showRouteInfo(route);
        }
    }

    private List<Station> findOptimalRoute(Station start, Station end) {
        Map<Station, Station> previous = new HashMap<>();
        Map<Station, Double> distances = new HashMap<>();
        PriorityQueue<Station> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (Station station : stations) {
            distances.put(station, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Station current = queue.poll();
            if (current == end) {
                break;
            }

            for (Station neighbor : current.getNeighbors()) {
                double distance = distances.get(current) + 1; // Assuming each station is 1 unit away
                if (distance < distances.get(neighbor)) {
                    distances.put(neighbor, distance);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
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
}