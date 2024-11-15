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

/**
 * The main activity of the application, responsible for initializing the metro map,
 * handling user interactions, and managing the display of station and route information.
 */
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

    private boolean isMetroMap = true; // Флаг для определения текущей карты

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load user preferences for map file and theme
        SharedPreferences sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String selectedMapFileName = sharedPreferences.getString("selected_map_file", "map_data.json");
        String selectedTheme = sharedPreferences.getString("selected_theme", "light");

        // Инициализация кнопки переключения карты
        Button switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> {
            isMetroMap = !isMetroMap;
            loadMetroData(selectedMapFileName ,isMetroMap ? "metro_map" : "suburban_map");
        });

        // Initialize the settings button and set its click listener
        ConstraintLayout settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Log.d("MainActivity", "Settings button clicked");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });

        // Initialize UI components
        metroMapView = findViewById(R.id.metroMapView);
        startStationEditText = findViewById(R.id.startStationEditText);
        endStationEditText = findViewById(R.id.endStationEditText);
        stationsRecyclerView = findViewById(R.id.stationsRecyclerView);

        // Initialize data structures
        stations = new ArrayList<>();
        lines = new ArrayList<>();
        selectedStations = new ArrayList<>();

        // Load metro data from the selected map file
        loadMetroData(selectedMapFileName, isMetroMap ? "metro_map" : "suburban_map"); // Загрузка карты метро по умолчанию

        // Initialize the RecyclerView for displaying stations
        stationsAdapter = new StationsAdapter(new ArrayList<Station>(), this);
        stationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stationsRecyclerView.setAdapter(stationsAdapter);

        // Set focus change listeners for the start and end station input fields
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

        // Set text change listeners for the start and end station input fields
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
    private List<MapObject> mapObjects; // List of map objects

    /**
     * Loads metro data from a JSON file. Parses the JSON data to populate the
     * stations, lines, transfers, rivers, and map objects.
     *
     * @param mapType The type of the map to load ("metro_map" or "suburban_map").
     */
    private void loadMetroData(String mapFileName, String mapType) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject mapData = jsonObject.getJSONObject(mapType);

            JSONArray linesArray = mapData.getJSONArray("lines");

            // Initialize lists
            stations = new ArrayList<>();
            lines = new ArrayList<>();
            transfers = new ArrayList<>();
            rivers = new ArrayList<>();
            mapObjects = new ArrayList<>();

            // First, load all stations and lines
            for (int i = 0; i < linesArray.length(); i++) {
                JSONObject lineObject = linesArray.getJSONObject(i);
                boolean isCircle = lineObject.optBoolean("isCircle", false);
                String lineType = lineObject.optString("lineType", "single"); // Добавлен параметр lineType
                Line line = new Line(lineObject.getInt("id"), lineObject.getString("name"), lineObject.getString("color"), isCircle, lineType);
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
            JSONArray transfersArray = mapData.getJSONArray("transfers");
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
                int width = riverObject.optInt("width", 10); // Default width is 10
                rivers.add(new River(riverPoints, width));
            }

            // Load intermediate points
            JSONArray intermediatePointsArray = mapData.getJSONArray("intermediatePoints");
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
            JSONArray objectsArray = mapData.getJSONArray("objects");
            for (int i = 0; i < objectsArray.length(); i++) {
                JSONObject objectObject = objectsArray.getJSONObject(i);
                String name = objectObject.getString("name");
                String displayName = objectObject.getString("displayName");
                String type = objectObject.getString("type");
                JSONObject positionObject = objectObject.getJSONObject("position");
                Point position = new Point(positionObject.getInt("x"), positionObject.getInt("y"));
                mapObjects.add(new MapObject(name, type, position, displayName));
            }

            // Set data to MetroMapView
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a JSONArray to a String array.
     *
     * @param array The JSONArray to convert.
     * @return A String array containing the elements of the JSONArray.
     */
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

    /**
     * Loads a JSON string from an asset file.
     *
     * @param mapFileName The name of the asset file.
     * @return The JSON string loaded from the asset file.
     */
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

    /**
     * Finds a station by its ID.
     *
     * @param id The ID of the station to find.
     * @return The station with the specified ID, or null if not found.
     */
    private Station findStationById(int id) {
        for (Station station : stations) {
            if (station.getId() == id) {
                return station;
            }
        }
        return null;
    }

    /**
     * Called when a station is clicked on the metro map.
     *
     * @param station The station that was clicked.
     */
    @Override
    public void onStationClick(Station station) {
        Station prevStation = null;
        Station nextStation = null;
        Line curline = null;

        // Find the previous and next stations on the same line
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

        // Clear the FrameLayout before adding a new fragment
        clearFrameLayout();

        // Create and add a new StationInfoFragment to the FrameLayout
        StationInfoFragment fragment = StationInfoFragment.newInstance(curline, station, prevStation, nextStation, transfers, lines);
        fragment.setOnStationInfoListener(this);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, fragment)
                .commit();
    }

    /**
     * Clears the FrameLayout by removing any existing fragment.
     */
    private void clearFrameLayout() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.frameLayout);
        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    /**
     * Called when the start station is set from the StationInfoFragment.
     *
     * @param station The station to set as the start station.
     */
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
            hideStationsList(); // Скрываем список станций, если станция была выбрана через StationInfoFragment
        }
    }

    /**
     * Called when the end station is set from the StationInfoFragment.
     *
     * @param station The station to set as the end station.
     */
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
            hideStationsList(); // Скрываем список станций, если станция была выбрана через StationInfoFragment
        }
    }

    /**
     * Finds the optimal route between two stations using Dijkstra's algorithm.
     *
     * @param start The starting station.
     * @param end   The destination station.
     * @return A list of stations representing the optimal route.
     */
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

    /**
     * Clears the start and end station input fields.
     */
    public void clearRouteInputs() {
        startStationEditText.setText("");
        endStationEditText.setText("");
    }

    /**
     * Displays the route information in a fragment.
     *
     * @param route The list of stations representing the route.
     */
    private void showRouteInfo(List<Station> route) {
        clearFrameLayout();
        RouteInfoFragment routeInfoFragment = RouteInfoFragment.newInstance(route, metroMapView, this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, routeInfoFragment)
                .commit();
    }

    /**
     * Shows the list of stations in the RecyclerView.
     *
     * @param stations The list of stations to display.
     */
    private void showStationsList(List<Station> stations) {
        stationsRecyclerView.setVisibility(View.VISIBLE);
        stationsAdapter.setStations(stations);
    }

    /**
     * Hides the list of stations in the RecyclerView.
     */
    private void hideStationsList() {
        stationsRecyclerView.setVisibility(View.GONE);
    }

    /**
     * Filters the list of stations based on the query string.
     *
     * @param query The query string to filter the stations.
     */
    private void filterStations(String query) {
        List<Station> filteredStations = new ArrayList<>();
        for (Station station : stations) {
            if (station.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredStations.add(station);
            }
        }
        stationsAdapter.setStations(filteredStations);
    }

    /**
     * Called when a station is selected from the RecyclerView.
     *
     * @param station The selected station.
     */
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