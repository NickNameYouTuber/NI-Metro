package com.nicorp.nimetro.presentation.activities;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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

import com.google.gson.JsonObject;
import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.models.River;
import com.nicorp.nimetro.domain.entities.APITariff;
import com.nicorp.nimetro.domain.entities.FlatRateTariff;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Tariff;
import com.nicorp.nimetro.domain.entities.Transfer;
import com.nicorp.nimetro.domain.entities.ZoneBasedTariff;
import com.nicorp.nimetro.presentation.adapters.StationSpinnerAdapter;
import com.nicorp.nimetro.presentation.fragments.AddTransferDialogFragment;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The activity responsible for editing the metro map. Allows users to add, remove,
 * and modify stations, lines, transfers, and other map elements.
 */
public class EditMapActivity extends AppCompatActivity implements AddTransferDialogFragment.OnTransferAddedListener {

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

    private boolean isMetroMap = true; // Флаг для определения текущей карты

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
        loadMetroData("metro_map"); // Загрузка карты метро по умолчанию
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

        // Инициализация кнопки переключения карты
        Button switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> {
            isMetroMap = !isMetroMap;
            loadMetroData(isMetroMap ? "metro_map" : "suburban_map");
        });

        addStationButton.setOnClickListener(v -> showAddStationDialog());
        removeStationButton.setOnClickListener(v -> removeSelectedStation());
        saveChangesButton.setOnClickListener(v -> saveMetroData());
        addTransferButton.setOnClickListener(v -> showAddTransferDialog());
        addIntermediatePointsButton.setOnClickListener(v -> showAddIntermediatePointsDialog());
    }

    private float lastTouchX;
    private float lastTouchY;
    private float originalStationX;
    private float originalStationY;

    /**
     * Sets up event listeners for touch handling.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupEventListeners() {
        metroMapView.setOnTouchListener((v, event) -> {
            // Find station or intermediate point at the touch location using transformed coordinates
            float[] point = new float[]{event.getX(), event.getY()};
            Matrix inverseMatrix = new Matrix();
            metroMapView.getTransformMatrix().invert(inverseMatrix);
            inverseMatrix.mapPoints(point);
            float x = point[0] / MetroMapView.COORDINATE_SCALE_FACTOR;
            float y = point[1] / MetroMapView.COORDINATE_SCALE_FACTOR;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selectedStation = metroMapView.findStationAt(x, y);
                    if (selectedStation == null) {
                        selectedIntermediatePoint = findIntermediatePointAt(x, y);
                    }
                    isMovingMap = (selectedStation == null && selectedIntermediatePoint == null);
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (selectedStation != null) {
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        selectedStation.setX((int) (selectedStation.getX() + dx / MetroMapView.COORDINATE_SCALE_FACTOR));
                        selectedStation.setY((int) (selectedStation.getY() + dy / MetroMapView.COORDINATE_SCALE_FACTOR));
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        metroMapView.needsRedraw = true;
                        metroMapView.invalidate(); // Перерисовываем карту
                    } else if (selectedIntermediatePoint != null) {
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        selectedIntermediatePoint.x = (int) (selectedIntermediatePoint.x + dx / MetroMapView.COORDINATE_SCALE_FACTOR);
                        selectedIntermediatePoint.y = (int) (selectedIntermediatePoint.y + dy / MetroMapView.COORDINATE_SCALE_FACTOR);
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        metroMapView.needsRedraw = true;
                        metroMapView.invalidate(); // Перерисовываем карту
                    } else if (isMovingMap) {
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        metroMapView.setTranslateX(metroMapView.getTranslateX() + dx / metroMapView.getScaleFactor());
                        metroMapView.setTranslateY(metroMapView.getTranslateY() + dy / metroMapView.getScaleFactor());
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        metroMapView.needsRedraw = true;
                        metroMapView.invalidate(); // Перерисовываем карту
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    selectedStation = null;
                    selectedIntermediatePoint = null;
                    isMovingMap = false;
                    metroMapView.needsRedraw = true;
                    metroMapView.invalidate(); // Перерисовываем карту
                    break;
            }

            // Allow MetroMapView to handle its own touch events if no station or point is selected
            boolean result = selectedStation != null || selectedIntermediatePoint != null || isMovingMap;
            metroMapView.scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    metroMapView.scaleFactor *= 2;
                    metroMapView.scaleFactor = Math.max(0.1f, Math.min(metroMapView.scaleFactor, 2.0f));
                    metroMapView.updateTransformMatrix();
                    metroMapView.needsRedraw = true; // Помечаем, что требуется перерисовка
                    metroMapView.invalidate();
                    return true;
                }
            });
            result = metroMapView.scaleGestureDetector.onTouchEvent(event) || result;
            return result;
        });
    }

    /**
     * Handles the ACTION_DOWN event.
     */
    public void handleActionDown(float mappedX, float mappedY, MotionEvent event) {
        // Convert the mapped coordinates to the coordinate system used by stations
        float stationX = mappedX / MetroMapView.COORDINATE_SCALE_FACTOR;
        float stationY = mappedY / MetroMapView.COORDINATE_SCALE_FACTOR;

        selectedStation = metroMapView.findStationAt(stationX, stationY);
        if (selectedStation == null) {
            selectedIntermediatePoint = findIntermediatePointAt(stationX, stationY);
        }

        initialTouchX = event.getX();
        initialTouchY = event.getY();
        isMovingMap = (selectedStation == null && selectedIntermediatePoint == null);
    }

    /**
     * Handles the ACTION_MOVE event.
     */
    public void handleActionMove(float mappedX, float mappedY, MotionEvent event) {
        // Convert the mapped coordinates to the coordinate system used by stations
        float stationX = mappedX / MetroMapView.COORDINATE_SCALE_FACTOR;
        float stationY = mappedY / MetroMapView.COORDINATE_SCALE_FACTOR;

        if (selectedStation != null) {
            selectedStation.setX((int) stationX);
            selectedStation.setY((int) stationY);
        } else if (selectedIntermediatePoint != null) {
            selectedIntermediatePoint.set((int) stationX, (int) stationY);
        } else if (isMovingMap) {
            // Calculate movement offsets in screen coordinates
            float deltaX = (event.getX() - initialTouchX) / metroMapView.getScaleFactor();
            float deltaY = (event.getY() - initialTouchY) / metroMapView.getScaleFactor();

            // Update map's translation
            metroMapView.setTranslateX(metroMapView.getTranslateX() + deltaX);
            metroMapView.setTranslateY(metroMapView.getTranslateY() + deltaY);

            // Update initial touch positions
            initialTouchX = event.getX();
            initialTouchY = event.getY();
        }
        metroMapView.invalidate(); // Перерисовываем карту
    }

    /**
     * Handles the ACTION_UP event.
     */
    public void handleActionUp() {
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
    private List<Line> allLines;

    public List<Line> getAllLines() {
        return allLines;
    }


    /**
     * Loads metro data from the JSON asset file.
     */
    private void loadMetroData(String mapType) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());
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
                    Station station = findStationById(stationId, stations);
                    if (station != null) {
                        transferStations.add(station);
                    }
                }
                int time = transferObject.optInt("time", 3);
                String type = transferObject.optString("type", "regular");
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


            // Объединяем станции и добавляем соседей
            List<Station> allStations = new ArrayList<>(stations);
                addNeighbors(mapData, allStations);

            allLines = new ArrayList<>(lines);

            // Set data to MetroMapView
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, new ArrayList<>(), new ArrayList<>());
        } catch (JSONException e) {
            e.printStackTrace();
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
     * @param stations The list of stations to search.
     * @return The station with the specified ID, or null if not found.
     */
    private Station findStationById(String id, List<Station> stations) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
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

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, new ArrayList<>(), new ArrayList<>());
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
                selectedLine.getId() + "_" + (selectedLine.getStations().size() + 1), // Unique ID
                stationName,
                selectedStation.getX() + 20,
                selectedStation.getY() + 20,
                "",
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
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, new ArrayList<>(), new ArrayList<>());
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
            infoObject.put("name", "Москва");
            infoObject.put("icon", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/%D0%9B%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF_%D0%BC%D0%B5%D1%82%D1%80%D0%BE_%D0%B2_%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B5_%D0%B1%D1%80%D0%B5%D0%BD%D0%B4%D0%B0_%D0%BC%D0%BE%D1%81%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%82%D1%80%D0%B0%D0%BD%D1%81%D0%BF%D0%BE%D1%80%D1%82%D0%B0.svg/330px-%D0%9B%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF_%D0%BC%D0%B5%D1%82%D1%80%D0%BE_%D0%B2_%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B5_%D0%B1%D1%80%D0%B5%D0%BD%D0%B4%D0%B0_%D0%BC%D0%BE%D1%81%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%82%D1%80%D0%B0%D0%BD%D1%81%D0%BF%D0%BE%D1%80%D1%82%D0%B0.svg.png");
            jsonObject.put("info", infoObject);

            // Save metro_map
            JSONObject metroMapObject = new JSONObject();
            JSONArray linesArray = new JSONArray();
            for (Line line : lines) {
                JSONObject lineObject = new JSONObject();
                lineObject.put("id", line.getId());
                lineObject.put("displayNumber", line.getId());
                lineObject.put("displayShape", "CIRCLE");
                lineObject.put("tariff",new JSONObject(line.getTariff().getJson().toString()));
                lineObject.put("name", line.getName());
                lineObject.put("color", line.getColor());
                lineObject.put("isCircle", line.isCircle());
                lineObject.put("lineType", line.getLineType());

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
            metroMapObject.put("lines", linesArray);

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
            metroMapObject.put("transfers", transfersArray);

            JSONArray intermediatePointsArray = new JSONArray();
            for (Station station : stations) {
                if (station.getIntermediatePoints() != null) {
                    for (Map.Entry<Station, List<Point>> entry : station.getIntermediatePoints().entrySet()) {
                        JSONObject intermediatePointObject = new JSONObject();
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
            metroMapObject.put("intermediatePoints", intermediatePointsArray);

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
            metroMapObject.put("rivers", riversArray);

            JSONArray objectsArray = new JSONArray();
            for (MapObject mapObject : mapObjects) {
                JSONObject objectObject = new JSONObject();
                objectObject.put("name", mapObject.getName());
                objectObject.put("displayNumber", mapObject.getdisplayNumber());
                objectObject.put("type", mapObject.getType());
                JSONObject positionObject = new JSONObject();
                positionObject.put("x", mapObject.getPosition().x);
                positionObject.put("y", mapObject.getPosition().y);
                objectObject.put("position", positionObject);
                objectsArray.put(objectObject);
            }
            metroMapObject.put("objects", objectsArray);

            jsonObject.put("metro_map", metroMapObject);

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

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, new ArrayList<>(), new ArrayList<>());
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
    public void showAddTransferDialog() {
        AddTransferDialogFragment dialog = new AddTransferDialogFragment(stations, this);
        dialog.show(getSupportFragmentManager(), "AddTransferDialogFragment");
    }

    @Override
    public void onTransferAdded(List<Station> selectedStations) {
        if (selectedStations.size() < 2) {
            Toast.makeText(this, "Выберите хотя бы две станции", Toast.LENGTH_SHORT).show();
            return;
        }

        // Добавляем станции друг к другу в соседи
        for (int i = 0; i < selectedStations.size(); i++) {
            for (int j = i + 1; j < selectedStations.size(); j++) {
                Station station1 = selectedStations.get(i);
                Station station2 = selectedStations.get(j);
                station1.addNeighbor(new Station.Neighbor(station2, 3));
                station2.addNeighbor(new Station.Neighbor(station1, 3));
            }
        }

        // Добавляем переход в список transfers
        transfers.add(new Transfer(selectedStations, 3, "default"));

        // Обновляем карту
        metroMapView.setData(lines, stations, transfers, rivers, mapObjects, new ArrayList<>(), new ArrayList<>());
        metroMapView.invalidate();
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
}