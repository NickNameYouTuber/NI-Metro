package com.nicorp.nimetro.presentation.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.presentation.adapters.StationSpinnerAdapter;
import com.nicorp.nimetro.presentation.views.MetroMapView;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.presentation.adapters.StationListAdapter;
import com.nicorp.nimetro.domain.entities.Facilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The activity responsible for editing the metro map. Allows users to add, remove,
 * and modify stations, lines, transfers, and other map elements.
 */
public class EditMapActivity extends AppCompatActivity {

    private static final String EDITED_DATA_FILENAME = "metro_data_edited.json";
    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects;

    private Station selectedStation;
    private Point selectedIntermediatePoint; // Variable for the selected intermediate point

    private float initialTouchX;
    private float initialTouchY;
    private boolean isMovingMap = false;

    /**
     * Called when the activity is first created. Initializes the UI components,
     * loads metro data, and sets up event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        initializeUIComponents();
        loadMetroData();
        setupEventListeners();
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUIComponents() {
        metroMapView = findViewById(R.id.editMetroMapView);
        metroMapView.setEditMode(true);
        Button addStationButton = findViewById(R.id.addStationButton);
        Button removeStationButton = findViewById(R.id.removeStationButton);
        Button saveChangesButton = findViewById(R.id.saveChangesButton);
        Button addTransferButton = findViewById(R.id.addTransferButton);
        Button addIntermediatePointsButton = findViewById(R.id.addIntermediatePointsButton);

        addStationButton.setOnClickListener(v -> showAddStationDialog());
        removeStationButton.setOnClickListener(v -> removeSelectedStation());
        saveChangesButton.setOnClickListener(v -> saveMetroData());
        addTransferButton.setOnClickListener(v -> showAddTransferDialog());
        addIntermediatePointsButton.setOnClickListener(v -> showAddIntermediatePointsDialog());
    }

    /**
     * Sets up event listeners for touch handling.
     */
    private void setupEventListeners() {
        metroMapView.setOnTouchListener((v, event) -> {
            float x = event.getX() / metroMapView.getScaleFactor() - metroMapView.getTranslateX();
            float y = event.getY() / metroMapView.getScaleFactor() - metroMapView.getTranslateY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleActionDown(x, y, event);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    handleActionMove(x, y, event);
                    return true;

                case MotionEvent.ACTION_UP:
                    handleActionUp();
                    return true;
            }
            return false;
        });
    }

    /**
     * Handles the ACTION_DOWN event.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param event The MotionEvent.
     */
    private void handleActionDown(float x, float y, MotionEvent event) {
        selectedStation = metroMapView.findStationAt(x / MetroMapView.COORDINATE_SCALE_FACTOR, y / MetroMapView.COORDINATE_SCALE_FACTOR);
        if (selectedStation == null) {
            selectedIntermediatePoint = findIntermediatePointAt(x / MetroMapView.COORDINATE_SCALE_FACTOR, y / MetroMapView.COORDINATE_SCALE_FACTOR);
        }
        initialTouchX = event.getX();
        initialTouchY = event.getY();
        isMovingMap = (selectedStation == null && selectedIntermediatePoint == null); // Only move map if no station or intermediate point is selected
    }

    /**
     * Handles the ACTION_MOVE event.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param event The MotionEvent.
     */
    private void handleActionMove(float x, float y, MotionEvent event) {
        if (selectedStation != null) {
            selectedStation.setX((int) (x / MetroMapView.COORDINATE_SCALE_FACTOR));
            selectedStation.setY((int) (y / MetroMapView.COORDINATE_SCALE_FACTOR));
        } else if (selectedIntermediatePoint != null) {
            selectedIntermediatePoint.set((int) (x / MetroMapView.COORDINATE_SCALE_FACTOR), (int) (y / MetroMapView.COORDINATE_SCALE_FACTOR));
        } else if (isMovingMap) {
            // Calculate movement offsets
            float deltaX = (event.getX() - initialTouchX) / metroMapView.getScaleFactor();
            float deltaY = (event.getY() - initialTouchY) / metroMapView.getScaleFactor();

            // Update map's translation
            metroMapView.setTranslateX(metroMapView.getTranslateX() + deltaX);
            metroMapView.setTranslateY(metroMapView.getTranslateY() + deltaY);

            // Update initial touch positions
            initialTouchX = event.getX();
            initialTouchY = event.getY();
        }
        metroMapView.invalidate(); // Redraw map view
    }

    /**
     * Handles the ACTION_UP event.
     */
    private void handleActionUp() {
        if (selectedStation != null) {
//                        selectedStation.snapToGrid();
        }
        selectedStation = null;
        selectedIntermediatePoint = null;
        isMovingMap = false;
        metroMapView.invalidate(); // Redraw map view
    }

    /**
     * Finds an intermediate point at the specified coordinates.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The intermediate point if found, otherwise null.
     */
    private Point findIntermediatePointAt(float x, float y) {
        for (Station station : stations) {
            if (station.getIntermediatePoints() != null) {
                for (Map.Entry<Station, List<Point>> entry : station.getIntermediatePoints().entrySet()) {
                    List<Point> intermediatePoints = entry.getValue();
                    for (Point point : intermediatePoints) {
                        if (Math.abs(point.x - x) < 10 / MetroMapView.COORDINATE_SCALE_FACTOR && Math.abs(point.y - y) < 10 / MetroMapView.COORDINATE_SCALE_FACTOR) {
                            return point;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Loads metro data from the JSON asset file.
     */
    private void loadMetroData() {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());

            JSONArray linesArray = jsonObject.getJSONArray("lines");

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
            JSONArray transfersArray = jsonObject.getJSONArray("transfers");
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
     * @return The JSON string loaded from the asset file.
     */
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
     * Shows a dialog to add a new station.
     */
    private void showAddStationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_station, null);
        builder.setView(dialogView);

        EditText stationNameEditText = dialogView.findViewById(R.id.stationNameEditText);
        Spinner lineSpinner = dialogView.findViewById(R.id.lineSpinner);
        Spinner stationSpinner = dialogView.findViewById(R.id.stationSpinner);
        Button createButton = dialogView.findViewById(R.id.createButton);

        // Initialize AlertDialog instance
        AlertDialog alertDialog = builder.create();

        // Set up line spinner
        List<String> lineNames = new ArrayList<>();
        for (Line line : lines) {
            lineNames.add(line.getName());
        }
        ArrayAdapter<String> lineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lineNames);
        lineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lineSpinner.setAdapter(lineAdapter);

        // Set up station spinner
        lineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Line selectedLine = lines.get(position);
                List<String> stationNames = new ArrayList<>();
                for (Station station : selectedLine.getStations()) {
                    stationNames.add(station.getName());
                }
                ArrayAdapter<String> stationAdapter = new ArrayAdapter<>(EditMapActivity.this, android.R.layout.simple_spinner_item, stationNames);
                stationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stationSpinner.setAdapter(stationAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Create button click handler
        createButton.setOnClickListener(v -> {
            String stationName = stationNameEditText.getText().toString();
            int selectedLineIndex = lineSpinner.getSelectedItemPosition();
            int selectedStationIndex = stationSpinner.getSelectedItemPosition();

            if (stationName.isEmpty()) {
                Toast.makeText(this, "Enter station name", Toast.LENGTH_SHORT).show();
                return;
            }

            Line selectedLine = lines.get(selectedLineIndex);
            Station selectedStation = selectedLine.getStations().get(selectedStationIndex);

            Station newStation = createNewStation(stationName, selectedLine, selectedStation);

            // Add new station to the list
            stations.add(newStation);
            selectedLine.getStations().add(newStation);

            // Add neighbor relationship
            newStation.addNeighbor(new Station.Neighbor(selectedStation, 2));
            selectedStation.addNeighbor(new Station.Neighbor(newStation, 2));

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
            metroMapView.invalidate();

            // Dismiss the dialog safely
            alertDialog.dismiss();
        });

        // Show the dialog
        alertDialog.show();
    }

    /**
     * Creates a new station.
     *
     * @param stationName The name of the new station.
     * @param selectedLine The selected line.
     * @param selectedStation The selected station.
     * @return The new station.
     */
    private Station createNewStation(String stationName, Line selectedLine, Station selectedStation) {
        return new Station(
                stations.size() + 1, // Unique ID
                stationName,
                selectedStation.getX() + 20,
                selectedStation.getY() + 20,
                selectedLine.getColor(),
                new Facilities("5:30 - 0:00", 0, 0, new String[]{}),
                0
        );
    }

    /**
     * Removes the selected station from the map.
     */
    private void removeSelectedStation() {
        if (selectedStation != null) {
            stations.remove(selectedStation);
            selectedStation = null;
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
            metroMapView.invalidate();
        } else {
            Toast.makeText(this, "Select a station to remove", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves the edited metro data to a JSON file.
     */
    private void saveMetroData() {
        try {
            JSONObject jsonObject = new JSONObject();

            // Save info
            JSONObject infoObject = new JSONObject();
            infoObject.put("version", "1.0");
            infoObject.put("author", "Nicorp");
            infoObject.put("country", "Россия");
            infoObject.put("name", "Самара");
            infoObject.put("icon", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Samara_Metro_logo.svg/351px-Samara_Metro_logo.svg.png");
            jsonObject.put("info", infoObject);

            // Save lines
            JSONArray linesArray = new JSONArray();
            for (Line line : lines) {
                JSONObject lineObject = new JSONObject();
                lineObject.put("id", line.getId());
                lineObject.put("name", line.getName());
                lineObject.put("color", line.getColor());
                lineObject.put("isCircle", line.isCircle());

                JSONArray stationsArray = new JSONArray();
                for (Station station : line.getStations()) {
                    JSONObject stationObject = new JSONObject();
                    stationObject.put("id", station.getId());
                    stationObject.put("name", station.getName());
                    stationObject.put("x", station.getX());
                    stationObject.put("y", station.getY());
                    stationObject.put("textPosition", station.getTextPosition());

                    JSONArray neighborsArray = new JSONArray();
                    for (Station.Neighbor neighbor : station.getNeighbors()) {
                        JSONArray neighborArray = new JSONArray();
                        neighborArray.put(neighbor.getStation().getId());
                        neighborArray.put(neighbor.getTime());
                        neighborsArray.put(neighborArray);
                    }
                    stationObject.put("neighbors", neighborsArray);

                    JSONObject facilitiesObject = new JSONObject();
                    facilitiesObject.put("schedule", station.getFacilities().getSchedule());
                    facilitiesObject.put("escalators", station.getFacilities().getEscalators());
                    facilitiesObject.put("elevators", station.getFacilities().getElevators());
                    facilitiesObject.put("exits", new JSONArray(station.getFacilities().getExits()));
                    stationObject.put("facilities", facilitiesObject);

                    stationsArray.put(stationObject);
                }
                lineObject.put("stations", stationsArray);
                linesArray.put(lineObject);
            }
            jsonObject.put("lines", linesArray);

            // Save transfers
            JSONArray transfersArray = new JSONArray();
            for (Transfer transfer : transfers) {
                JSONObject transferObject = new JSONObject();
                JSONArray stationsArray = new JSONArray();
                for (Station station : transfer.getStations()) {
                    stationsArray.put(station.getId());
                }
                transferObject.put("stations", stationsArray);
                transferObject.put("time", transfer.getTime());
                transferObject.put("type", transfer.getType());
                transfersArray.put(transferObject);
            }
            jsonObject.put("transfers", transfersArray);

            // Save intermediate points
            JSONArray intermediatePointsArray = new JSONArray();
            for (Station station : stations) {
                if (station.getIntermediatePoints() != null) {
                    for (Map.Entry<Station, List<Point>> entry : station.getIntermediatePoints().entrySet()) {
                        JSONObject intermediatePointObject = new JSONObject();

                        // Save neighborsId like "neighborsId": [88, 89]" from station.getId() and entry.getKey().getId()
                        JSONArray neighborsIds = new JSONArray();
                        neighborsIds.put(station.getId());
                        neighborsIds.put(entry.getKey().getId());

                        intermediatePointObject.put("neighborsId", neighborsIds);

                        JSONArray pointsArray = new JSONArray();
                        for (Point point : entry.getValue()) {
                            JSONObject pointObject = new JSONObject();
                            pointObject.put("x", point.x);
                            pointObject.put("y", point.y);
                            pointsArray.put(pointObject);
                        }
                        intermediatePointObject.put("points", pointsArray);
                        intermediatePointsArray.put(intermediatePointObject);
                    }
                }
            }
            jsonObject.put("intermediatePoints", intermediatePointsArray);

            // Save rivers
            JSONArray riversArray = new JSONArray();
            for (River river : rivers) {
                JSONObject riverObject = new JSONObject();
                JSONArray pointsArray = new JSONArray();
                for (Point point : river.getPoints()) {
                    JSONObject pointObject = new JSONObject();
                    pointObject.put("x", point.x);
                    pointObject.put("y", point.y);
                    pointsArray.put(pointObject);
                }
                riverObject.put("points", pointsArray);
                riverObject.put("width", river.getWidth());
                riversArray.put(riverObject);
            }
            jsonObject.put("rivers", riversArray);

            // Save map objects
            JSONArray objectsArray = new JSONArray();
            for (MapObject mapObject : mapObjects) {
                JSONObject objectObject = new JSONObject();
                objectObject.put("name", mapObject.getName());
                objectObject.put("displayName", mapObject.getDisplayName());
                objectObject.put("type", mapObject.getType());
                JSONObject positionObject = new JSONObject();
                positionObject.put("x", mapObject.getPosition().x);
                positionObject.put("y", mapObject.getPosition().y);
                objectObject.put("position", positionObject);
                objectsArray.put(objectObject);
            }
            jsonObject.put("objects", objectsArray);

            // Save JSON data to internal storage file
            OutputStream os = new FileOutputStream(new File(getFilesDir(), EDITED_DATA_FILENAME));
            os.write(jsonObject.toString().getBytes());
            os.close();

            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save error", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  Shows a dialog for adding intermediate points between two stations.
     *  The dialog has two spinners for selecting the stations and a button to add the intermediate points.
     *  The dialog is displayed in the center of the screen.*
     */
    private void showAddIntermediatePointsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_intermediate_points, null);
        builder.setView(dialogView);

        Spinner station1Spinner = dialogView.findViewById(R.id.station1Spinner);
        Spinner station2Spinner = dialogView.findViewById(R.id.station2Spinner);
        Button addIntermediatePointsButton = dialogView.findViewById(R.id.addIntermediatePointsButton);

        // Initialize AlertDialog instance
        AlertDialog alertDialog = builder.create();

        // Set up station spinners
        StationSpinnerAdapter stationAdapter = new StationSpinnerAdapter(this, stations);
        station1Spinner.setAdapter(stationAdapter);
        station2Spinner.setAdapter(stationAdapter);

        // Add intermediate points button click handler
        addIntermediatePointsButton.setOnClickListener(v -> {
            int selectedStation1Index = station1Spinner.getSelectedItemPosition();
            int selectedStation2Index = station2Spinner.getSelectedItemPosition();

            if (selectedStation1Index == selectedStation2Index) {
                Toast.makeText(this, "Select different stations", Toast.LENGTH_SHORT).show();
                return;
            }

            Station station1 = stations.get(selectedStation1Index);
            Station station2 = stations.get(selectedStation2Index);

            // Add intermediate points between the two stations
            List<Point> intermediatePoints = new ArrayList<>();
            intermediatePoints.add(new Point(station1.getX() + 10, station1.getY() + 10));
            intermediatePoints.add(new Point(station2.getX() - 10, station2.getY() - 10));

            station1.addIntermediatePoints(station2, intermediatePoints);

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
            metroMapView.invalidate();

            // Dismiss the dialog safely
            alertDialog.dismiss();
        });

        // Show the dialog
        alertDialog.show();
    }

    /**
     * Shows a dialog to add a new transfer.
     */
    private void showAddTransferDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_transfer, null);
        builder.setView(dialogView);

        TextView transferListLabel = dialogView.findViewById(R.id.transferListLabel);
        LinearLayout transferListContainer = dialogView.findViewById(R.id.transferListContainer);
        Button addStationButton = dialogView.findViewById(R.id.addStationButton);
        Button saveTransferButton = dialogView.findViewById(R.id.saveTransferButton);

        // Initialize AlertDialog instance
        AlertDialog alertDialog = builder.create();

        // Set up add station button
        addStationButton.setOnClickListener(v -> {
            AlertDialog.Builder stationPickerBuilder = new AlertDialog.Builder(this);
            stationPickerBuilder.setTitle("Select a station");

            StationListAdapter stationAdapter = new StationListAdapter(this, stations);
            stationPickerBuilder.setAdapter(stationAdapter, (dialog, which) -> {
                Station selectedStation = stations.get(which);
                View stationItemView = LayoutInflater.from(this).inflate(R.layout.item_station_list, null);
                TextView stationNameTextView = stationItemView.findViewById(R.id.stationNameTextView);
                TextView stationIdTextView = stationItemView.findViewById(R.id.stationIdTextView);

                stationNameTextView.setText(selectedStation.getName());
                stationIdTextView.setText(String.valueOf(selectedStation.getId()));

                transferListContainer.addView(stationItemView);
            });

            // Add search functionality
            EditText searchEditText = new EditText(this);
            searchEditText.setHint("Search station");
            stationPickerBuilder.setView(searchEditText);

            stationPickerBuilder.setPositiveButton("OK", null);
            stationPickerBuilder.setNegativeButton("Cancel", null);

            AlertDialog stationPickerDialog = stationPickerBuilder.create();
            stationPickerDialog.setOnShowListener(dialog -> {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        stationAdapter.filter(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            });

            stationPickerDialog.show();
        });

        // Set up save transfer button
        saveTransferButton.setOnClickListener(v -> {
            List<Station> selectedStations = new ArrayList<>();
            for (int i = 0; i < transferListContainer.getChildCount(); i++) {
                View stationItemView = transferListContainer.getChildAt(i);
                TextView stationIdTextView = stationItemView.findViewById(R.id.stationIdTextView);
                int stationId = Integer.parseInt(stationIdTextView.getText().toString());

                for (Station station : stations) {
                    if (station.getId() == stationId) {
                        selectedStations.add(station);
                        break;
                    }
                }
            }

            if (selectedStations.size() < 2) {
                Toast.makeText(this, "Select at least two stations to create a transfer", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create new transfer
            Transfer newTransfer = new Transfer(selectedStations, 2, "regular");
            transfers.add(newTransfer);

            // Add neighbor relationships
            for (int i = 0; i < selectedStations.size(); i++) {
                for (int j = i + 1; j < selectedStations.size(); j++) {
                    selectedStations.get(i).addNeighbor(new Station.Neighbor(selectedStations.get(j), 2));
                    selectedStations.get(j).addNeighbor(new Station.Neighbor(selectedStations.get(i), 2));
                }
            }

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
            metroMapView.invalidate();

            // Dismiss the dialog safely
            alertDialog.dismiss();
        });

        // Show the dialog
        alertDialog.show();
    }
}