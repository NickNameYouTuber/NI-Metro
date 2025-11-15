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
import android.widget.CheckBox;
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
    private List<Station> tramStations;
    private List<Line> tramLines;
    private List<Transfer> tramTransfers;
    private List<River> tramRivers;
    private List<MapObject> tramMapObjects;

    private Station selectedStation;
    private Point selectedIntermediatePoint;

    private float initialTouchX;
    private float initialTouchY;
    private boolean isMovingMap = false;

    private boolean isMetroMap = true;
    private boolean isSuburbanMap = false;
    private boolean isRiverTramMap = false;
    private boolean isTramMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        metroMapView = findViewById(R.id.editMetroMapView);
        metroMapView.setEditMode(true);
        metroMapView.setOnStationClickListener(new MetroMapView.OnStationClickListener() {
            @Override
            public void onStationClick(Station station) {
                selectedStation = station;
                selectedIntermediatePoint = null;
                showStationEditDialog(station);
            }
        });

        loadMapData();
        setupMapTypeButtons();
        setupSaveButton();
        setupTouchHandlers();
    }

    private void setupMapTypeButtons() {
        Button switchMapButton = findViewById(R.id.switchMapButton);
        switchMapButton.setOnClickListener(v -> {
            if (isMetroMap) {
                isMetroMap = false;
                isSuburbanMap = true;
                isRiverTramMap = false;
                isTramMap = false;
            } else if (isSuburbanMap) {
                isMetroMap = false;
                isSuburbanMap = false;
                isRiverTramMap = true;
                isTramMap = false;
            } else if (isRiverTramMap) {
                isMetroMap = false;
                isSuburbanMap = false;
                isRiverTramMap = false;
                isTramMap = true;
            } else {
                isMetroMap = true;
                isSuburbanMap = false;
                isRiverTramMap = false;
                isTramMap = false;
            }
            updateMapData();
        });
    }

    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.saveChangesButton);
        saveButton.setOnClickListener(v -> saveMapData());
    }

    private void setupTouchHandlers() {
        metroMapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getX();
                    initialTouchY = event.getY();
                    isMovingMap = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isMovingMap) {
                        float deltaX = event.getX() - initialTouchX;
                        float deltaY = event.getY() - initialTouchY;
                        metroMapView.setTranslateX(metroMapView.getTranslateX() + deltaX);
                        metroMapView.setTranslateY(metroMapView.getTranslateY() + deltaY);
                        metroMapView.invalidate();
                        initialTouchX = event.getX();
                        initialTouchY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isMovingMap = false;
                    break;
            }
            return true;
        });
    }

    private void updateMapData() {
        if (isMetroMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    true, false, false, false
            );
        } else if (isSuburbanMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, true, false, false
            );
        } else if (isRiverTramMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, false, true, false
            );
        } else if (isTramMap) {
            metroMapView.setData(
                    lines, stations, transfers, rivers, mapObjects,
                    suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects,
                    riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects,
                    tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects,
                    false, false, false, true
            );
        }
    }

    private void loadMetroData(String mapFileName) {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset(mapFileName));
            JSONObject metroMapData = jsonObject.optJSONObject("metro_map");
            JSONObject suburbanMapData = jsonObject.optJSONObject("suburban_map");
            JSONObject tramMapData = jsonObject.optJSONObject("tram_map");
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
            if (tramMapData != null) {
                loadMapData(tramMapData, tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects);
            }

            List<Station> allStations = new ArrayList<>();
            allStations.addAll(stations);
                allStations.addAll(suburbanStations);
                allStations.addAll(riverTramStations);
                allStations.addAll(tramStations);

                addNeighbors(metroMapData, allStations);
                addNeighbors(suburbanMapData, allStations);
                addNeighbors(riverTramMapData, allStations);
            addNeighbors(tramMapData, allStations);

            updateMapData();
        } catch (JSONException e) {
            Log.e("EditMapActivity", "Error loading map data", e);
        }
    }

    private void loadMapData() {
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
        tramStations = new ArrayList<>();
        tramLines = new ArrayList<>();
        tramTransfers = new ArrayList<>();
        tramRivers = new ArrayList<>();
        tramMapObjects = new ArrayList<>();

        loadMetroData("metro_data.json");
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
            String lineId = lineObject.getString("id");
            String lineName = lineObject.getString("name");
            String lineColor = lineObject.getString("color");
            String lineType = lineObject.optString("type", "single");

            JSONArray stationsArray = lineObject.getJSONArray("stations");
            List<Station> lineStations = new ArrayList<>();
            for (int j = 0; j < stationsArray.length(); j++) {
                JSONObject stationObject = stationsArray.getJSONObject(j);
                Station station = findStationById(stationObject.getString("id"), stations);
                if (station != null) {
                    lineStations.add(station);
                        } else {
                    Station newStation = new Station(
                        stationObject.getString("id"),
                        stationObject.getString("name"),
                        stationObject.getInt("x"),
                        stationObject.getInt("y"),
                        stationObject.optString("ESP", ""),
                        stationObject.optString("color", ""),
                        null,
                        stationObject.optInt("textPosition", 0)
                    );
                    newStation.setDisplayNumber(stationObject.optString("displayNumber", ""));
                    stations.add(newStation);
                    lineStations.add(newStation);
            }
            }

            boolean isCircle = "circle".equals(lineType);
            Line line = new Line(lineId, lineName, lineColor, isCircle, lineType, null, "", "");
            line.getStations().addAll(lineStations);
            lines.add(line);
        }

        JSONArray transfersArray = mapData.optJSONArray("transfers");
        if (transfersArray != null) {
        for (int i = 0; i < transfersArray.length(); i++) {
            JSONObject transferObject = transfersArray.getJSONObject(i);
            JSONArray stationsArray = transferObject.getJSONArray("stations");
            List<Station> transferStations = new ArrayList<>();
            for (int j = 0; j < stationsArray.length(); j++) {
                    Station station = findStationById(stationsArray.getString(j), stations);
                if (station != null) {
                    transferStations.add(station);
                }
            }
                if (!transferStations.isEmpty()) {
                    transfers.add(new Transfer(transferStations, transferObject.optInt("time", 3), transferObject.optString("type", "default")));
        }
            }
        }

        JSONArray riversArray = mapData.optJSONArray("rivers");
        if (riversArray != null) {
            for (int i = 0; i < riversArray.length(); i++) {
                JSONObject riverObject = riversArray.getJSONObject(i);
                    JSONArray pointsArray = riverObject.getJSONArray("points");
                List<Point> points = new ArrayList<>();
                    for (int j = 0; j < pointsArray.length(); j++) {
                        JSONObject pointObject = pointsArray.getJSONObject(j);
                    points.add(new Point(pointObject.getInt("x"), pointObject.getInt("y")));
                        }
                int width = riverObject.optInt("width", 10);
                rivers.add(new River(points, width));
            }
        }

        JSONArray mapObjectsArray = mapData.optJSONArray("mapObjects");
        if (mapObjectsArray != null) {
            for (int i = 0; i < mapObjectsArray.length(); i++) {
                JSONObject mapObjectObject = mapObjectsArray.getJSONObject(i);
                Point position = new Point(mapObjectObject.getInt("x"), mapObjectObject.getInt("y"));
                MapObject mapObject = new MapObject(
                        mapObjectObject.getString("id"),
                        mapObjectObject.getString("type"),
                        position,
                        mapObjectObject.optString("displayNumber", "")
                );
                mapObjects.add(mapObject);
            }
        }
    }

    private Station findStationById(String id, List<Station> stations) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }

    private void showStationEditDialog(Station station) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_station, null);
        builder.setView(dialogView);

        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        EditText xEditText = dialogView.findViewById(R.id.xEditText);
        EditText yEditText = dialogView.findViewById(R.id.yEditText);
        EditText textPositionEditText = dialogView.findViewById(R.id.textPositionEditText);
        EditText displayNumberEditText = dialogView.findViewById(R.id.displayNumberEditText);

        nameEditText.setText(station.getName());
        xEditText.setText(String.valueOf(station.getX()));
        yEditText.setText(String.valueOf(station.getY()));
        textPositionEditText.setText(String.valueOf(station.getTextPosition()));
        displayNumberEditText.setText(station.getdisplayNumber());

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                station.setName(nameEditText.getText().toString());
                station.setX(Integer.parseInt(xEditText.getText().toString()));
                station.setY(Integer.parseInt(yEditText.getText().toString()));
                station.setTextPosition(Integer.parseInt(textPositionEditText.getText().toString()));
                station.setDisplayNumber(displayNumberEditText.getText().toString());
                updateMapData();
                metroMapView.invalidate();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Ошибка ввода данных", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void saveMapData() {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject metroMapData = createMapDataJson(lines, stations, transfers, rivers, mapObjects);
            JSONObject suburbanMapData = createMapDataJson(suburbanLines, suburbanStations, suburbanTransfers, suburbanRivers, suburbanMapObjects);
            JSONObject riverTramMapData = createMapDataJson(riverTramLines, riverTramStations, riverTramTransfers, riverTramRivers, riverTramMapObjects);
            JSONObject tramMapData = createMapDataJson(tramLines, tramStations, tramTransfers, tramRivers, tramMapObjects);

            jsonObject.put("metro_map", metroMapData);
            jsonObject.put("suburban_map", suburbanMapData);
            jsonObject.put("rivertram_map", riverTramMapData);
            jsonObject.put("tram_map", tramMapData);

            File file = new File(getFilesDir(), EDITED_DATA_FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonObject.toString().getBytes());
            fos.close();

            Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            Log.e("EditMapActivity", "Error saving map data", e);
            Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
        }
    }

    private JSONObject createMapDataJson(List<Line> lines, List<Station> stations, List<Transfer> transfers, List<River> rivers, List<MapObject> mapObjects) throws JSONException {
            JSONObject jsonObject = new JSONObject();

            JSONArray linesArray = new JSONArray();
            for (Line line : lines) {
                JSONObject lineObject = new JSONObject();
                lineObject.put("id", line.getId());
                lineObject.put("name", line.getName());
                lineObject.put("color", line.getColor());
            lineObject.put("type", line.getType());

                JSONArray stationsArray = new JSONArray();
                for (Station station : line.getStations()) {
                stationsArray.put(station.getId());
            }
            lineObject.put("stations", stationsArray);
            linesArray.put(lineObject);
        }
        jsonObject.put("lines", linesArray);

        JSONArray stationsArray = new JSONArray();
        for (Station station : stations) {
                    JSONObject stationObject = new JSONObject();
                    stationObject.put("id", station.getId());
                    stationObject.put("name", station.getName());
                    stationObject.put("x", station.getX());
                    stationObject.put("y", station.getY());
                        stationObject.put("textPosition", station.getTextPosition());
            stationObject.put("displayNumber", station.getdisplayNumber());

                    JSONArray neighborsArray = new JSONArray();
            if (station.getNeighbors() != null) {
                    for (Station.Neighbor neighbor : station.getNeighbors()) {
                        JSONArray neighborArray = new JSONArray();
                        neighborArray.put(neighbor.getStation().getId());
                        neighborArray.put(neighbor.getTime());
                        neighborsArray.put(neighborArray);
                }
                    }
                    stationObject.put("neighbors", neighborsArray);

            if (station.getIntermediatePoints() != null && !station.getIntermediatePoints().isEmpty()) {
                JSONObject intermediatePointsObject = new JSONObject();
                for (Map.Entry<Station, List<Point>> entry : station.getIntermediatePoints().entrySet()) {
                    JSONArray pointsArray = new JSONArray();
                    for (Point point : entry.getValue()) {
                        JSONObject pointObject = new JSONObject();
                        pointObject.put("x", point.x);
                        pointObject.put("y", point.y);
                        pointsArray.put(pointObject);
                    }
                    intermediatePointsObject.put(entry.getKey().getId(), pointsArray);
                }
                stationObject.put("intermediatePoints", intermediatePointsObject);
            }

                    stationsArray.put(stationObject);
                }
        jsonObject.put("stations", stationsArray);

            JSONArray transfersArray = new JSONArray();
            for (Transfer transfer : transfers) {
                JSONObject transferObject = new JSONObject();
            JSONArray stationsArrayForTransfer = new JSONArray();
                for (Station station : transfer.getStations()) {
                stationsArrayForTransfer.put(station.getId());
                }
            transferObject.put("stations", stationsArrayForTransfer);
                transferObject.put("time", transfer.getTime());
                transferObject.put("type", transfer.getType());
                transfersArray.put(transferObject);
            }
        jsonObject.put("transfers", transfersArray);

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
                riversArray.put(riverObject);
            }
        jsonObject.put("rivers", riversArray);

        JSONArray mapObjectsArray = new JSONArray();
            for (MapObject mapObject : mapObjects) {
            JSONObject mapObjectObject = new JSONObject();
            mapObjectObject.put("id", mapObject.getId());
            mapObjectObject.put("type", mapObject.getType());
            mapObjectObject.put("x", mapObject.getPosition().x);
            mapObjectObject.put("y", mapObject.getPosition().y);
            mapObjectObject.put("displayNumber", mapObject.getdisplayNumber());
            mapObjectsArray.put(mapObjectObject);
            }
        jsonObject.put("mapObjects", mapObjectsArray);

        return jsonObject;
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e("EditMapActivity", "Error loading JSON from asset", ex);
        }
        return json;
    }

    private void addStationToLine(Line line, Station station) {
        line.getStations().add(station);
        updateMapData();
        metroMapView.invalidate();
    }

    private void removeStationFromLine(Line line, Station station) {
        line.getStations().remove(station);
        updateMapData();
            metroMapView.invalidate();
    }

    private void addLine(Line line) {
        lines.add(line);
        updateMapData();
        metroMapView.invalidate();
        }

    private void removeLine(Line line) {
        lines.remove(line);
        updateMapData();
        metroMapView.invalidate();
    }

    public void onStationSelected(Station station) {
        selectedStation = station;
        showStationEditDialog(station);
    }

    public void onStationAdded(Station station) {
        stations.add(station);
        updateMapData();
        metroMapView.invalidate();
    }

    public void onStationRemoved(Station station) {
        stations.remove(station);
        for (Line line : lines) {
            line.getStations().remove(station);
            }
        updateMapData();
        metroMapView.invalidate();
    }

    public void onLineAdded(Line line) {
        lines.add(line);
        updateMapData();
        metroMapView.invalidate();
            }

    public void onLineRemoved(Line line) {
        lines.remove(line);
        updateMapData();
        metroMapView.invalidate();
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

        updateMapData();
        metroMapView.invalidate();
    }

    private void addNeighbors(JSONObject mapData, List<Station> allStations) throws JSONException {
        if (mapData == null) return;
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

                    if (stationObject.has("intermediatePoints")) {
                        JSONObject intermediatePointsObject = stationObject.getJSONObject("intermediatePoints");
                        Iterator<String> keys = intermediatePointsObject.keys();
                        while (keys.hasNext()) {
                            String neighborId = keys.next();
                            JSONArray pointsArray = intermediatePointsObject.getJSONArray(neighborId);
                            List<Point> intermediatePoints = new ArrayList<>();
                            for (int k = 0; k < pointsArray.length(); k++) {
                                JSONObject pointObject = pointsArray.getJSONObject(k);
                                intermediatePoints.add(new Point(pointObject.getInt("x"), pointObject.getInt("y")));
                            }
                        Station neighborStation = findStationById(neighborId, allStations);
                        if (neighborStation != null) {
                                station.addIntermediatePoints(neighborStation, intermediatePoints);
                            }
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
