package com.nicorp.nimetro;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MetroMapView.OnStationClickListener, StationInfoDialogFragment.OnStationInfoListener {

    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private Station selectedStartStation;
    private Station selectedEndStation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        metroMapView = findViewById(R.id.metroMapView);
        stations = new ArrayList<>();
        lines = new ArrayList<>();

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
                Line line = new Line(lineObject.getInt("id"), lineObject.getString("name"), lineObject.getString("color"));
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

    @Override
    public void onStationClick(Station station) {
        StationInfoDialogFragment dialogFragment = StationInfoDialogFragment.newInstance(station);
        dialogFragment.setOnStationInfoListener(this);
        dialogFragment.show(getSupportFragmentManager(), "station_info");
    }

    @Override
    public void onSetStart(Station station) {
        selectedStartStation = station;
    }

    @Override
    public void onSetEnd(Station station) {
        selectedEndStation = station;
        if (selectedStartStation != null) {
            List<Station> route = findOptimalRoute(selectedStartStation, selectedEndStation);
            metroMapView.setRoute(route);
            showRouteInfo(route);
        }
    }

    private List<Station> findOptimalRoute(Station start, Station end) {
        // Implement your route finding algorithm here
        // This is a placeholder implementation
        List<Station> route = new ArrayList<>();
        route.add(start);
        route.add(end);
        return route;
    }

    private void showRouteInfo(List<Station> route) {
        RouteInfoDialogFragment dialogFragment = RouteInfoDialogFragment.newInstance(route);
        dialogFragment.show(getSupportFragmentManager(), "route_info");
    }
}