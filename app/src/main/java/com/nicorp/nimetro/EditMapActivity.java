package com.nicorp.nimetro;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class EditMapActivity extends AppCompatActivity {

    private static final String EDITED_DATA_FILENAME = "metro_data_edited.json";
    private MetroMapView metroMapView;
    private List<Station> stations;
    private List<Line> lines;
    private List<Transfer> transfers;
    private List<River> rivers;
    private List<MapObject> mapObjects;

    private Station selectedStation;

    private float initialTouchX;
    private float initialTouchY;
    private boolean isMovingMap = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        metroMapView = findViewById(R.id.editMetroMapView);
        Button addStationButton = findViewById(R.id.addStationButton);
        Button removeStationButton = findViewById(R.id.removeStationButton);
        Button saveChangesButton = findViewById(R.id.saveChangesButton);
        Button addTransferButton = findViewById(R.id.addTransferButton); // Добавляем кнопку для создания переходов

        // Load and set metro data
        loadMetroData();
        metroMapView.setData(lines, stations, transfers, rivers, mapObjects);

        // Button click handlers
        addStationButton.setOnClickListener(v -> showAddStationDialog());
        removeStationButton.setOnClickListener(v -> removeSelectedStation());
        saveChangesButton.setOnClickListener(v -> saveMetroData());
        addTransferButton.setOnClickListener(v -> showAddTransferDialog()); // Обработчик для создания переходов

        // Touch handling
        metroMapView.setOnTouchListener((v, event) -> {
            float x = event.getX() / metroMapView.getScaleFactor() - metroMapView.getTranslateX();
            float y = event.getY() / metroMapView.getScaleFactor() - metroMapView.getTranslateY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selectedStation = metroMapView.findStationAt(x / MetroMapView.COORDINATE_SCALE_FACTOR, y / MetroMapView.COORDINATE_SCALE_FACTOR);
                    initialTouchX = event.getX();
                    initialTouchY = event.getY();
                    isMovingMap = (selectedStation == null); // Only move map if no station is selected
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (selectedStation != null) {
                        selectedStation.setX((int) (x / MetroMapView.COORDINATE_SCALE_FACTOR));
                        selectedStation.setY((int) (y / MetroMapView.COORDINATE_SCALE_FACTOR));
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
                    return true;

                case MotionEvent.ACTION_UP:
                    if (selectedStation != null) {
                        selectedStation.snapToGrid();
                    }
                    selectedStation = null;
                    isMovingMap = false;
                    metroMapView.invalidate(); // Redraw map view
                    return true;
            }
            return false;
        });
    }

    private void loadMetroData() {
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());
            JSONArray linesArray = jsonObject.getJSONArray("lines");

            stations = new ArrayList<>();
            lines = new ArrayList<>();
            transfers = new ArrayList<>();
            rivers = new ArrayList<>();
            mapObjects = new ArrayList<>();

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

            // Load map objects
            JSONArray mapObjectsArray = jsonObject.getJSONArray("mapObjects");
            for (int i = 0; i < mapObjectsArray.length(); i++) {
                JSONObject mapObjectObject = mapObjectsArray.getJSONObject(i);
                String name = mapObjectObject.getString("name");
                String type = mapObjectObject.getString("type");
                String displayName = mapObjectObject.getString("displayName");
                JSONObject positionObject = mapObjectObject.getJSONObject("position");
                Point position = new Point(positionObject.getInt("x"), positionObject.getInt("y"));
                mapObjects.add(new MapObject(type, displayName, position, name));
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
                Toast.makeText(this, "Введите название станции", Toast.LENGTH_SHORT).show();
                return;
            }

            Line selectedLine = lines.get(selectedLineIndex);
            Station selectedStation = selectedLine.getStations().get(selectedStationIndex);

            Station newStation = new Station(
                    stations.size() + 1, // Unique ID
                    stationName,
                    selectedStation.getX() + 20,
                    selectedStation.getY() + 20,
                    selectedLine.getColor(),
                    new Facilities("5:30 - 0:00", 0, 0, new String[]{}),
                    0
            );

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

    private void removeSelectedStation() {
        if (selectedStation != null) {
            stations.remove(selectedStation);
            selectedStation = null;
            metroMapView.setData(lines, stations, transfers, rivers, mapObjects);
            metroMapView.invalidate();
        } else {
            Toast.makeText(this, "Выберите станцию для удаления", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMetroData() {
        try {
            JSONObject jsonObject = new JSONObject();
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

            // Save JSON data to internal storage file
            OutputStream os = new FileOutputStream(new File(getFilesDir(), EDITED_DATA_FILENAME));
            os.write(jsonObject.toString().getBytes());
            os.close();

            Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
        }
    }

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
            stationPickerBuilder.setTitle("Выберите станцию");

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
            searchEditText.setHint("Поиск станции");
            stationPickerBuilder.setView(searchEditText);

            stationPickerBuilder.setPositiveButton("ОК", null);
            stationPickerBuilder.setNegativeButton("Отмена", null);

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
                Toast.makeText(this, "Выберите хотя бы две станции для создания перехода", Toast.LENGTH_SHORT).show();
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