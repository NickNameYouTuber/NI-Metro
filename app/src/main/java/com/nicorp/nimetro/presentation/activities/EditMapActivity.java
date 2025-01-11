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

public class EditMapActivity extends AppCompatActivity implements AddTransferDialogFragment.OnTransferAddedListener {

    private static final String EDITED_DATA_FILENAME = "metro_data_edited.json";
    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects;

    private List<Station> suburbanStations;
    private List<Line> suburbanLines;
    private List<Transfer> suburbanTransfers;
    private List<River> suburbanRivers;
    private List<MapObject> suburbanMapObjects;

    private List<Station> riverTramStations;
    private List<Line> riverTramLines;
    private List<Transfer> riverTramTransfers;
    private List<River> riverTramRivers;
    private List<MapObject> riverTramMapObjects;

    private Station selectedStation;
    private Point selectedIntermediatePoint;

    private float initialTouchX;
    private float initialTouchY;
    private boolean isMovingMap = false;

    private boolean isMetroMap = true;
    private boolean isSuburbanMap = false;
    private boolean isRiverTramMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        // Инициализация всех списков
        stations = new ArrayList<>();
        lines = new ArrayList<>();
        transfers = new ArrayList<>();
        rivers = new ArrayList<>();
        mapObjects = new ArrayList<>();

        suburbanStations = new ArrayList<>();
        suburbanLines = new ArrayList<>();
        suburbanTransfers = new ArrayList<>();
        suburbanRivers = new ArrayList<>();
        suburbanMapObjects = new ArrayList<>();

        riverTramStations = new ArrayList<>();
        riverTramLines = new ArrayList<>();
        riverTramTransfers = new ArrayList<>();
        riverTramRivers = new ArrayList<>();
        riverTramMapObjects = new ArrayList<>();

        initializeUIComponents();
        loadMetroData("metromap_1.json"); // Загрузка карты метро по умолчанию
        setupEventListeners();
    }

    private void initializeUIComponents() {
        metroMapView = findViewById(R.id.editMetroMapView);
        metroMapView.setEditMode(true);
        Button addStationButton = findViewById(R.id.addStationButton);
        Button removeStationButton = findViewById(R.id.removeStationButton);
        Button saveChangesButton = findViewById(R.id.saveChangesButton);
        Button addTransferButton = findViewById(R.id.addTransferButton);
        Button addIntermediatePointsButton = findViewById(R.id.addIntermediatePointsButton);

        Button switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> {
            if (isMetroMap) {
                isMetroMap = false;
                isSuburbanMap = false;
                isRiverTramMap = true;
//                switchMapButton.setImageResource(R.drawable.river_tram_icon);
            } else if (isSuburbanMap) {
                isMetroMap = true;
                isSuburbanMap = false;
                isRiverTramMap = false;
//                switchMapButton.setImageResource(R.drawable.metro_map_icon);
            } else {
                isMetroMap = false;
                isSuburbanMap = true;
                isRiverTramMap = false;
//                switchMapButton.setImageResource(R.drawable.suburban_map_icon);
            }
            updateMapData(); // Обновляем данные в MetroMapView
        });

        addStationButton.setOnClickListener(v -> showAddStationDialog());
        removeStationButton.setOnClickListener(v -> removeSelectedStation());
        saveChangesButton.setOnClickListener(v -> saveMetroData());
        addTransferButton.setOnClickListener(v -> showAddTransferDialog());
        addIntermediatePointsButton.setOnClickListener(v -> showAddIntermediatePointsDialog());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupEventListeners() {
        metroMapView.setOnTouchListener((v, event) -> {
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
                        // Вычисляем смещение
                        float dx = (event.getRawX() - initialTouchX) / metroMapView.getScaleFactor();
                        float dy = (event.getRawY() - initialTouchY) / metroMapView.getScaleFactor();

                        // Обновляем координаты станции
                        selectedStation.setX((int) (selectedStation.getX() + dx));
                        selectedStation.setY((int) (selectedStation.getY() + dy));

                        // Обновляем начальные координаты для следующего перемещения
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        // Обновляем данные в MetroMapView и перерисовываем карту
                        updateMapData();
                    } else if (selectedIntermediatePoint != null) {
                        // Аналогично для промежуточных точек
                        float dx = (event.getRawX() - initialTouchX) / metroMapView.getScaleFactor();
                        float dy = (event.getRawY() - initialTouchY) / metroMapView.getScaleFactor();

                        selectedIntermediatePoint.x = (int) (selectedIntermediatePoint.x + dx);
                        selectedIntermediatePoint.y = (int) (selectedIntermediatePoint.y + dy);

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        // Обновляем данные в MetroMapView и перерисовываем карту
                        updateMapData();
                    } else if (isMovingMap) {
                        // Перемещение всей карты
                        float dx = (event.getRawX() - initialTouchX) / metroMapView.getScaleFactor();
                        float dy = (event.getRawY() - initialTouchY) / metroMapView.getScaleFactor();

                        metroMapView.setTranslateX(metroMapView.getTranslateX() + dx);
                        metroMapView.setTranslateY(metroMapView.getTranslateY() + dy);

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        // Перерисовываем карту
                        metroMapView.invalidate();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    selectedStation = null;
                    selectedIntermediatePoint = null;
                    isMovingMap = false;
                    metroMapView.invalidate();
                    break;
            }

            // Обработка масштабирования
            metroMapView.scaleGestureDetector.onTouchEvent(event);
            return true;
        });
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

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject metroMapData = jsonObject.optJSONObject("metro_map");
            JSONObject suburbanMapData = jsonObject.optJSONObject("suburban_map");
            JSONObject riverTramMapData = jsonObject.optJSONObject("rivertram_map");

            if (metroMapData != null) {
                loadMapData(metroMapData, lines, stations, transfers, rivers, mapObjects);
            }

            if (suburbanMapData != null) {
                loadMapData(suburbanMapData, suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects);
            }

            if (riverTramMapData != null) {
                loadMapData(riverTramMapData, riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects);
            }

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

            updateMapData();
        } catch (JSONException e) {
            e.printStackTrace();
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
                String ESP = stationObject.optString("ESP", null);

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

        if (mapData.has("rivers")) {
            JSONArray riversArray = mapData.getJSONArray("rivers");
            for (int i = 0; i < riversArray.length(); i++) {
                JSONObject riverObject = riversArray.getJSONObject(i);
                if (riverObject.has("points")) {
                    JSONArray pointsArray = riverObject.getJSONArray("points");
                    List<Point> riverPoints = new ArrayList<>();
                    for (int j = 0; j < pointsArray.length(); j++) {
                        JSONObject pointObject = pointsArray.getJSONObject(j);
                        if (pointObject.has("x") && pointObject.has("y")) {
                            int x = pointObject.getInt("x");
                            int y = pointObject.getInt("y");
                            Point point = new Point(x, y);
                            riverPoints.add(point);
                        }
                    }
                    int width = riverObject.optInt("width", 10);
                    rivers.add(new River(riverPoints, width));
                }
            }
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

    private void showAddStationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_station, null);
        builder.setView(dialogView);

        EditText stationNameEditText = dialogView.findViewById(R.id.stationNameEditText);
        Spinner lineSpinner = dialogView.findViewById(R.id.lineSpinner);
        Spinner stationSpinner = dialogView.findViewById(R.id.stationSpinner);
        Button createButton = dialogView.findViewById(R.id.createButton);

        AlertDialog alertDialog = builder.create();

        List<String> lineNames = new ArrayList<>();
        for (Line line : lines) {
            lineNames.add(line.getName());
        }
        ArrayAdapter<String> lineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lineNames);
        lineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lineSpinner.setAdapter(lineAdapter);

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
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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

            stations.add(newStation);
            selectedLine.getStations().add(newStation);

            newStation.addNeighbor(new Station.Neighbor(selectedStation, 2));
            selectedStation.addNeighbor(new Station.Neighbor(newStation, 2));

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, suburbanLines, suburbanStations);
            metroMapView.invalidate();

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private Station createNewStation(String stationName, Line selectedLine, Station selectedStation) {
        return new Station(
                selectedLine.getId() + "_" + (selectedLine.getStations().size() + 1),
                stationName,
                selectedStation.getX() + 20,
                selectedStation.getY() + 20,
                "",
                selectedLine.getColor(),
                new Facilities("5:30 - 0:00", 0, 0, new String[]{}),
                0
        );
    }

    private void removeSelectedStation() {
        if (selectedStation != null) {
            stations.remove(selectedStation);
            selectedStation = null;
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, suburbanLines, suburbanStations);
            metroMapView.invalidate();
        } else {
            Toast.makeText(this, "Select a station to remove", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMetroData() {
        try {
            JSONObject jsonObject = new JSONObject();

            JSONObject infoObject = new JSONObject();
            infoObject.put("version", "1.0");
            infoObject.put("author", "Nicorp");
            infoObject.put("country", "Россия");
            infoObject.put("name", "Москва");
            infoObject.put("icon", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/%D0%9B%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF_%D0%BC%D0%B5%D1%82%D1%80%D0%BE_%D0%B2_%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B5_%D0%B1%D1%80%D0%B5%D0%BD%D0%B4%D0%B0_%D0%BC%D0%BE%D1%81%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%82%D1%80%D0%B0%D0%BD%D1%81%D0%BF%D0%BE%D1%80%D1%82%D0%B0.svg/330px-%D0%9B%D0%BE%D0%B3%D0%BE%D1%82%D0%B8%D0%BF_%D0%BC%D0%B5%D1%82%D1%80%D0%BE_%D0%B2_%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B5_%D0%B1%D1%80%D0%B5%D0%BD%D0%B4%D0%B0_%D0%BC%D0%BE%D1%81%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%82%D1%80%D0%B0%D0%BD%D1%81%D0%BF%D0%BE%D1%80%D1%82%D0%B0.svg.png");
            jsonObject.put("info", infoObject);

            JSONObject metroMapObject = new JSONObject();
            JSONArray linesArray = new JSONArray();
            for (Line line : lines) {
                JSONObject lineObject = new JSONObject();
                lineObject.put("id", line.getId());
                lineObject.put("displayNumber", line.getId());
                lineObject.put("displayShape", "CIRCLE");
                lineObject.put("tariff", new JSONObject(line.getTariff().getJson().toString()));
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

            OutputStream os = new FileOutputStream(new File(getFilesDir(), EDITED_DATA_FILENAME));
            os.write(jsonObject.toString().getBytes());
            os.close();

            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save error", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddIntermediatePointsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_intermediate_points, null);
        builder.setView(dialogView);

        Spinner station1Spinner = dialogView.findViewById(R.id.station1Spinner);
        Spinner station2Spinner = dialogView.findViewById(R.id.station2Spinner);
        Button addIntermediatePointsButton = dialogView.findViewById(R.id.addIntermediatePointsButton);

        AlertDialog alertDialog = builder.create();

        StationSpinnerAdapter stationAdapter = new StationSpinnerAdapter(this, stations);
        station1Spinner.setAdapter(stationAdapter);
        station2Spinner.setAdapter(stationAdapter);

        addIntermediatePointsButton.setOnClickListener(v -> {
            int selectedStation1Index = station1Spinner.getSelectedItemPosition();
            int selectedStation2Index = station2Spinner.getSelectedItemPosition();

            if (selectedStation1Index == selectedStation2Index) {
                Toast.makeText(this, "Select different stations", Toast.LENGTH_SHORT).show();
                return;
            }

            Station station1 = stations.get(selectedStation1Index);
            Station station2 = stations.get(selectedStation2Index);

            List<Point> intermediatePoints = new ArrayList<>();
            intermediatePoints.add(new Point(station1.getX() + 10, station1.getY() + 10));
            intermediatePoints.add(new Point(station2.getX() - 10, station2.getY() - 10));

            station1.addIntermediatePoints(station2, intermediatePoints);

            metroMapView.setData(lines, stations, transfers, rivers, mapObjects, suburbanLines, suburbanStations);
            metroMapView.invalidate();

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

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

        for (int i = 0; i < selectedStations.size(); i++) {
            for (int j = i + 1; j < selectedStations.size(); j++) {
                Station station1 = selectedStations.get(i);
                Station station2 = selectedStations.get(j);
                station1.addNeighbor(new Station.Neighbor(station2, 3));
                station2.addNeighbor(new Station.Neighbor(station1, 3));
            }
        }

        transfers.add(new Transfer(selectedStations, 3, "default"));

        metroMapView.setData(lines, stations, transfers, rivers, mapObjects, suburbanLines, suburbanStations);
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
}