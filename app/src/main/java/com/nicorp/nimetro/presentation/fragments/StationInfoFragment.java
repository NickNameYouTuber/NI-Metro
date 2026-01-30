package com.nicorp.nimetro.presentation.fragments;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.domain.entities.CircleShape;
import com.nicorp.nimetro.domain.entities.DoubleCircleShape;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.ParallelogramShape;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StationInfoFragment extends Fragment {

    private static final String ARG_LINE = "line";
    private static final String ARG_STATION = "station";
    private static final String ARG_PREV_STATION = "prev_station";
    private static final String ARG_NEXT_STATION = "next_station";
    private static final String ARG_TRANSFER = "transfers";
    private static final String ARG_LINES = "lines";
    private static final String ARG_GRAYED_LINES = "grayed_lines";

    private Line line;
    private List<Line> lines;
    private List<Line> grayedLines;
    private Station station;
    private List<Transfer> transfers;
    private Station prevStation;
    private Station nextStation;
    private OnStationInfoListener listener;
    private Line selectedLineForStation;

    public static StationInfoFragment newInstance(Line line, Station station, Station prevStation, Station nextStation, List<Transfer> transfers, List<Line> lines, List<Line> grayedLines) {
        StationInfoFragment fragment = new StationInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_STATION, station);
        args.putParcelable(ARG_PREV_STATION, prevStation);
        args.putParcelable(ARG_NEXT_STATION, nextStation);
        args.putParcelable(ARG_LINE, line);
        args.putSerializable(ARG_TRANSFER, (Serializable) transfers);
        args.putSerializable(ARG_LINES, (Serializable) lines);
        args.putSerializable(ARG_GRAYED_LINES, (Serializable) grayedLines);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            station = getArguments().getParcelable(ARG_STATION);
            prevStation = getArguments().getParcelable(ARG_PREV_STATION);
            nextStation = getArguments().getParcelable(ARG_NEXT_STATION);
            line = getArguments().getParcelable(ARG_LINE);
            transfers = (List<Transfer>) getArguments().getSerializable(ARG_TRANSFER);
            lines = (List<Line>) getArguments().getSerializable(ARG_LINES);
            grayedLines = (List<Line>) getArguments().getSerializable(ARG_GRAYED_LINES);
        }
    }

    LinearLayout stationInfoContainer;
    TextView prevStationArrivalTime;
    TextView nextStationArrivalTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_station_info, container, false);

        prevStationArrivalTime = view.findViewById(R.id.prevStationArrivalTime);
        nextStationArrivalTime = view.findViewById(R.id.nextStationArrivalTime);

        // Контейнеры для списков соседних станций
        LinearLayout upperStationsContainer = view.findViewById(R.id.upperStationsContainer);
        LinearLayout lowerStationsContainer = view.findViewById(R.id.lowerStationsContainer);

        updateNeighborViews(upperStationsContainer, lowerStationsContainer);

        // Остальная логика
        TextView stationName = view.findViewById(R.id.stationName);
        stationName.setText(station.getName());
        Log.d("StationInfoFragmentInfo", "Station name: " + station.getName());

        TextView fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> onFromButtonClick());

        TextView toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> onToButtonClick());

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        setupLinesList(view, station);

        fetchESPSchedule(station);

        return view;
    }

    private void fetchESPSchedule(Station station) {
        String apiKey = "30512dc5-ba33-4a6d-8fba-9a7927cc1ef3";
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("StationInfoFragment", "Fetching ESP schedule for station: " + station.getName() + " on date: " + date);

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);

        // Запрос для предыдущей станции
        if (prevStation != null) {
            String fromPrev = station.getESP();
            String toPrev = prevStation.getESP();
            Call<YandexRaspResponse> callPrev = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, fromPrev, toPrev, "esr", date, 1000);
            callPrev.enqueue(new Callback<YandexRaspResponse>() {
                @Override
                public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                        if (!segments.isEmpty()) {
                            List<String> nearestArrivalTimePrev = findNearestArrivalTime(segments, prevStation);
                            Log.d("StationInfoFragment", "Nearest ESP arrival time prev " + prevStation.getName() + " " + prevStation.getESP() + " : " + nearestArrivalTimePrev);
                            // Отобразить время прибытия в UI
                            showESPArrivalTime(nearestArrivalTimePrev, null);
                        } else {
                            Log.d("StationInfoFragment", "No segments found for ESP schedule for prev station");
                        }
                    } else {
                        Log.e("StationInfoFragment", "Failed to fetch ESP schedule for prev station: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                    Log.e("StationInfoFragment", "Error fetching ESP schedule for prev station", t);
                }
            });
        }

        // Запрос для следующей станции
        if (nextStation != null) {
            String fromNext = station.getESP();
            String toNext = nextStation.getESP();
            Call<YandexRaspResponse> callNext = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, fromNext, toNext, "esr", date, 1000);
            callNext.enqueue(new Callback<YandexRaspResponse>() {
                @Override
                public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                        if (!segments.isEmpty()) {
                            List<String> nearestArrivalTimeNext = findNearestArrivalTime(segments, nextStation);
                            Log.d("StationInfoFragment", "Nearest ESP arrival time next " + nextStation.getName() + " " + nextStation.getESP() + " : " + nearestArrivalTimeNext);
                            // Отобразить время прибытия в UI
                            showESPArrivalTime(null, nearestArrivalTimeNext);
                        } else {
                            Log.d("StationInfoFragment", "No segments found for ESP schedule for next station");
                        }
                    } else {
                        Log.e("StationInfoFragment", "Failed to fetch ESP schedule for next station: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                    Log.e("StationInfoFragment", "Error fetching ESP schedule for next station", t);
                }
            });
        }
    }

    private List<String> findNearestArrivalTime(List<YandexRaspResponse.Segment> segments, Station targetStation) {
        List<String> nearestArrivalTimes = new ArrayList<>();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        for (YandexRaspResponse.Segment segment : segments) {
            try {
                Log.d("StationInfoFragment", "Arrival: " + segment.getArrival());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                ZonedDateTime arrivalDateTime = ZonedDateTime.parse(segment.getArrival(), formatter);
                Log.d("StationInfoFragment", "Arrival date: " + arrivalDateTime);
                Log.d("StationInfoFragment", "Current time: " + currentTime);
                long difference = ChronoUnit.MINUTES.between(currentTime, arrivalDateTime);
                long anotherDifference = ChronoUnit.MINUTES.between(arrivalDateTime, currentTime);
                Log.d("StationInfoFragment", "Difference: " + difference + " anotherDifference: " + anotherDifference);

                // Учитываем только положительные разницы
                if (difference > 0 && nearestArrivalTimes.size() < 3) {
                    nearestArrivalTimes.add(segment.getArrival().substring(11, 16));
                }
            } catch (Exception e) {
                Log.e("StationInfoFragment", "Error parsing date", e);
            }
        }

        return nearestArrivalTimes;
    }

    private void showESPArrivalTime(List<String> arrivalTimePrev, List<String> arrivalTimeNext) {
        if (arrivalTimePrev != null) {
            prevStationArrivalTime.setText(String.join(", ", arrivalTimePrev)); // Ошибка здесь
        }
        if (arrivalTimeNext != null) {
            nextStationArrivalTime.setText(String.join(", ", arrivalTimeNext)); // И здесь
        }
    }

    private void updateNeighborViews(LinearLayout upperContainer, LinearLayout lowerContainer) {
        if (upperContainer != null) {
            upperContainer.removeAllViews();
            upperContainer.setVisibility(View.GONE);
        }
        if (lowerContainer != null) {
            lowerContainer.removeAllViews();
            lowerContainer.setVisibility(View.GONE);
        }

        Line currentLine = findLineByStation(station);
        if (currentLine == null) {
            currentLine = line;
        }

        if (currentLine != null && currentLine.isCircle()) {
            applyCircularNeighbors(upperContainer, lowerContainer, currentLine);
            return;
        }

        applyDirectionalNeighbors(upperContainer, lowerContainer, currentLine);
    }

    private void applyDirectionalNeighbors(LinearLayout upperContainer, LinearLayout lowerContainer, Line currentLine) {
        List<Station> neighbors = getNeighborStations(currentLine, station);
        List<Station> aboveStations = new ArrayList<>();
        List<Station> sameLevelStations = new ArrayList<>();
        List<Station> belowStations = new ArrayList<>();

        if (neighbors != null) {
            for (Station neighbor : neighbors) {
                if (neighbor == null || neighbor.getId() == null || station == null || station.getId() == null) {
                    continue;
                }
                if (neighbor.getId().equals(station.getId())) {
                    continue;
                }
                int deltaY = neighbor.getY() - station.getY();
                if (deltaY < 0) {
                    aboveStations.add(neighbor);
                } else if (deltaY > 0) {
                    belowStations.add(neighbor);
                } else {
                    sameLevelStations.add(neighbor);
                }
            }
        }

        aboveStations.sort((a, b) -> {
            int deltaA = Math.abs(a.getY() - station.getY());
            int deltaB = Math.abs(b.getY() - station.getY());
            if (deltaA != deltaB) {
                return Integer.compare(deltaA, deltaB);
            }
            return compareStationsById(a, b);
        });

        belowStations.sort((a, b) -> {
            int deltaA = Math.abs(a.getY() - station.getY());
            int deltaB = Math.abs(b.getY() - station.getY());
            if (deltaA != deltaB) {
                return Integer.compare(deltaA, deltaB);
            }
            return compareStationsById(a, b);
        });

        sameLevelStations.sort(this::compareStationsById);

        if (!aboveStations.isEmpty()) {
            this.prevStation = aboveStations.get(0);
        } else {
            this.prevStation = null;
        }

        if (!belowStations.isEmpty()) {
            this.nextStation = belowStations.get(0);
        } else if (!sameLevelStations.isEmpty()) {
            this.nextStation = sameLevelStations.get(0);
        } else {
            this.nextStation = null;
        }

        for (Station neighbor : aboveStations) {
            addNeighborView(upperContainer, neighbor, null);
        }

        for (Station neighbor : sameLevelStations) {
            addNeighborView(lowerContainer, neighbor, null);
        }

        for (Station neighbor : belowStations) {
            addNeighborView(lowerContainer, neighbor, null);
        }

        if ((aboveStations.isEmpty()) && prevStationArrivalTime != null) {
            prevStationArrivalTime.setText("");
        }
        if ((belowStations.isEmpty() && sameLevelStations.isEmpty()) && nextStationArrivalTime != null) {
            nextStationArrivalTime.setText("");
        }
    }

    private void applyCircularNeighbors(LinearLayout upperContainer, LinearLayout lowerContainer, Line currentLine) {
        List<Station> stationList = currentLine.getStations();
        int index = findStationIndexById(stationList, station);
        Station previous = null;
        Station next = null;
        if (index != -1 && stationList != null && !stationList.isEmpty()) {
            previous = stationList.get((index - 1 + stationList.size()) % stationList.size());
            next = stationList.get((index + 1) % stationList.size());
        }

        this.prevStation = previous;
        this.nextStation = next;

        addNeighborView(upperContainer, previous, R.drawable.ic_clockwise);
        addNeighborView(lowerContainer, next, R.drawable.ic_counter_clockwise);

        if (previous == null && prevStationArrivalTime != null) {
            prevStationArrivalTime.setText("");
        }
        if (next == null && nextStationArrivalTime != null) {
            nextStationArrivalTime.setText("");
        }
    }

    private void addNeighborView(LinearLayout container, Station neighbor, Integer overrideArrowRes) {
        if (container == null || neighbor == null) {
            return;
        }
        TextView textView = createNeighborTextView(container.getContext());
        textView.setText(neighbor.getName());

        int drawableRes = overrideArrowRes != null ? overrideArrowRes : getDirectionArrowDrawable(neighbor);
        Drawable arrowDrawable = AppCompatResources.getDrawable(requireContext(), drawableRes);
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(arrowDrawable, null, null, null);

        int tintColor = textView.getCurrentTextColor();
        textView.setCompoundDrawableTintList(ColorStateList.valueOf(tintColor));

        container.addView(textView);
        container.setVisibility(View.VISIBLE);
    }

    private TextView createNeighborTextView(android.content.Context context) {
        TextView textView = new TextView(context);
        int textColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnBackground, Color.BLACK);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setCompoundDrawablePadding(dpToPx(4));
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.emyslabaltblack);
        if (customTypeface != null) {
            textView.setTypeface(customTypeface, Typeface.NORMAL);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(4);
        textView.setLayoutParams(params);
        return textView;
    }

    private int getDirectionArrowDrawable(Station neighbor) {
        if (neighbor == null || station == null) {
            return R.drawable.ic_arrow_dir_n;
        }
        float dx = neighbor.getX() - station.getX();
        float dy = neighbor.getY() - station.getY();
        if (dx == 0 && dy == 0) {
            return R.drawable.ic_arrow_dir_n;
        }

        double angle = Math.atan2(-dy, dx);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) {
            degrees += 360.0;
        }

        if (degrees >= 337.5 || degrees < 22.5) {
            return R.drawable.ic_arrow_dir_e;
        } else if (degrees < 67.5) {
            return R.drawable.ic_arrow_dir_ne;
        } else if (degrees < 112.5) {
            return R.drawable.ic_arrow_dir_n;
        } else if (degrees < 157.5) {
            return R.drawable.ic_arrow_dir_nw;
        } else if (degrees < 202.5) {
            return R.drawable.ic_arrow_dir_w;
        } else if (degrees < 247.5) {
            return R.drawable.ic_arrow_dir_sw;
        } else if (degrees < 292.5) {
            return R.drawable.ic_arrow_dir_s;
        } else {
            return R.drawable.ic_arrow_dir_se;
        }
    }

    private List<Station> getNeighborStations(Line currentLine, Station currentStation) {
        List<Station> neighbors = new ArrayList<>();
        if (currentStation == null) {
            return neighbors;
        }
        if (currentLine == null) {
            return neighbors;
        }

        List<Station.Neighbor> neighborEntries = currentStation.getNeighbors();
        if (neighborEntries != null) {
            Set<String> seenIds = new LinkedHashSet<>();
            for (Station.Neighbor neighborEntry : neighborEntries) {
                if (neighborEntry == null || neighborEntry.getStation() == null) {
                    continue;
                }
                String neighborId = neighborEntry.getStation().getId();
                if (neighborId == null || !seenIds.add(neighborId)) {
                    continue;
                }
                Station resolved = findStationInLineById(currentLine, neighborId);
                if (resolved != null) {
                    neighbors.add(resolved);
                }
            }
        }

        if (neighbors.isEmpty() && currentLine != null) {
            List<Station> sorted = getSortedStations(currentLine);
            Station prev = findNeighborById(sorted, currentStation, true);
            Station next = findNeighborById(sorted, currentStation, false);
            if (prev != null) {
                neighbors.add(prev);
            }
            if (next != null) {
                neighbors.add(next);
            }
        }
        return neighbors;
    }

    private Station findStationInLineById(Line line, String stationId) {
        if (line == null || stationId == null || line.getStations() == null) {
            return null;
        }
        for (Station candidate : line.getStations()) {
            if (candidate != null && stationId.equals(candidate.getId())) {
                return candidate;
            }
        }
        return null;
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrRes, typedValue, true)) {
            return typedValue.data;
        }
        return Color.WHITE;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private List<Station> getSortedStations(Line sourceLine) {
        if (sourceLine == null || sourceLine.getStations() == null) {
            return Collections.emptyList();
        }
        List<Station> sorted = new ArrayList<>();
        for (Station entry : sourceLine.getStations()) {
            if (entry != null) {
                sorted.add(entry);
            }
        }
        sorted.sort(this::compareStationsById);
        return sorted;
    }

    private int compareStationsById(Station first, Station second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return 1;
        }
        if (second == null) {
            return -1;
        }
        String idFirst = first.getId();
        String idSecond = second.getId();
        int numericCompare = Integer.compare(extractNumericSuffix(idFirst), extractNumericSuffix(idSecond));
        if (numericCompare != 0) {
            return numericCompare;
        }
        if (idFirst == null && idSecond == null) {
            return 0;
        }
        if (idFirst == null) {
            return 1;
        }
        if (idSecond == null) {
            return -1;
        }
        return idFirst.compareTo(idSecond);
    }

    private int extractNumericSuffix(String id) {
        if (id == null || id.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int index = id.length() - 1;
        int start = id.length();
        while (index >= 0 && Character.isDigit(id.charAt(index))) {
            start = index;
            index--;
        }
        if (start == id.length()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(id.substring(start));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private Station findNeighborById(List<Station> sortedStations, Station current, boolean previous) {
        if (sortedStations == null || current == null || current.getId() == null) {
            return null;
        }
        String targetId = current.getId();
        for (int i = 0; i < sortedStations.size(); i++) {
            Station candidate = sortedStations.get(i);
            if (candidate != null && targetId.equals(candidate.getId())) {
                int neighborIndex = previous ? i - 1 : i + 1;
                if (neighborIndex >= 0 && neighborIndex < sortedStations.size()) {
                    return sortedStations.get(neighborIndex);
                }
                break;
            }
        }
        return null;
    }

    private Station findNeighborByGeometry(List<Station> stations, Station current, boolean searchAbove) {
        if (stations == null || current == null) {
            return null;
        }
        int referenceY = current.getY();
        Station best = null;
        int bestDelta = Integer.MAX_VALUE;
        for (Station candidate : stations) {
            if (candidate == null || candidate.getId() == null || current.getId() == null) {
                continue;
            }
            if (candidate.getId().equals(current.getId())) {
                continue;
            }
            int delta = candidate.getY() - referenceY;
            if (searchAbove && delta >= 0) {
                continue;
            }
            if (!searchAbove && delta <= 0) {
                continue;
            }
            int absDelta = Math.abs(delta);
            if (best == null || absDelta < bestDelta) {
                best = candidate;
                bestDelta = absDelta;
            } else if (absDelta == bestDelta && compareStationsById(candidate, best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private int findStationIndexById(List<Station> stations, Station target) {
        if (stations == null || target == null || target.getId() == null) {
            return -1;
        }
        for (int i = 0; i < stations.size(); i++) {
            Station candidate = stations.get(i);
            if (candidate != null && target.getId().equals(candidate.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void onFromButtonClick() {
        if (listener != null) {
            listener.onSetStart(station, null, true);
        }
        dismiss();
    }

    private void onToButtonClick() {
        if (listener != null) {
            listener.onSetEnd(station, null, true);
        }
        dismiss();
    }

    private void setLineNumberAndColor(TextView lineNumber, View lineColorStrip, Station station) {
        Line foundLine = line;
        if (foundLine == null) {
            foundLine = findLineByStation(station);
        }
        
        if (foundLine == null) {
            lineNumber.setText("");
            return;
        }
        
        // Используем getLineDisplayNumberForStation для правильного получения displayNumber
        String displayNumber = foundLine.getLineDisplayNumberForStation(station);
        
        // Если displayNumber не найден для станции, используем общий displayNumber линии
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = foundLine.getdisplayNumber();
        }
        
        // Если displayNumber все еще null или пустой, используем id
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = foundLine.getId();
        }
        
        lineNumber.setText(displayNumber != null ? displayNumber : "");


        Shape shape = getLineShapeForStation(station);
        float density = getResources().getDisplayMetrics().density;

        // Создаем LayerDrawable для комбинации отступов и формы
        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);
        shapeDrawable.getPaint().setColor(parseColorOrDefault(station.getColor(), Color.BLACK));

        // Добавляем отступы к drawable
        int margin = (int) (8 * density);
        Drawable[] layers = new Drawable[]{shapeDrawable};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(0, margin, margin, 0, 0); // left, top, right, bottom

        lineNumber.setBackground(layerDrawable);

//        // Остальной код остается тем же
//        lineNumber.setGravity(Gravity.CENTER);
//        if (shape instanceof DoubleCircleShape) {
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    (int) (30*density),
//                    (int) (30*density)
//            );
//            lineNumber.setLayoutParams(params);
//            lineNumber.setTextColor(Color.BLACK);
//            lineNumber.setTextSize(14);
//        } else if (shape instanceof ParallelogramShape) {
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    (int) (40*density),
//                    (int) (30*density)
//            );
//            lineNumber.setLayoutParams(params);
//            lineNumber.setTextColor(Color.WHITE);
//            lineNumber.setTextSize(14);
//        } else {
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                    (int) (30*density),
//                    (int) (30*density)
//            );
//            lineNumber.setLayoutParams(params);
//            lineNumber.setTextColor(Color.WHITE);
//            lineNumber.setTextSize(15);
//        }

        Typeface customTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        lineNumber.setTypeface(customTypeface, Typeface.BOLD);
        lineNumber.setPadding(4, 4, 4, 4);
    }

    private void addTransferCircles(LinearLayout transferCirclesContainer) {
        if (transferCirclesContainer == null || transfers == null || station == null) {
            return;
        }
        Set<String> seenStationKeys = new LinkedHashSet<>();
        for (Transfer transfer : transfers) {
            if (transfer == null) {
                continue;
            }
            List<Station> transferStations = transfer.getStations();
            if (transferStations == null || !transferStations.contains(station)) {
                continue;
            }
            for (Station transferStation : transferStations) {
                if (transferStation == null || transferStation.equals(station)) {
                    continue;
                }
                String transferName = transferStation.getName();
                if (transferName == null || transferName.trim().isEmpty()) {
                    continue;
                }
                String key = buildStationKey(transferStation);
                if (!seenStationKeys.add(key)) {
                    continue;
                }
                Log.d("StationInfoFragment", "Transfer station: " + transferStation);
                TextView transferCircle = createTransferCircle(transferStation);
                transferCirclesContainer.addView(transferCircle);
            }
        }
    }

    private TextView createTransferCircle(Station transferStation) {
        TextView transferCircle = new TextView(getContext());
        String transferLineId = getLineIdForStation(transferStation);
        if (transferLineId == null || transferLineId.trim().isEmpty()) {
            transferLineId = transferStation.getName();
        }
        transferCircle.setText(transferLineId != null ? transferLineId : "");

        Shape shape = getLineShapeForStation(transferStation);
        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);
        shapeDrawable.getPaint().setColor(parseColorOrDefault(getColorForStation(transferStation), Color.BLACK));
        float density = getResources().getDisplayMetrics().density;
        shapeDrawable.setIntrinsicHeight((int) (24 * density)); // Установите нужную высоту
        shapeDrawable.setIntrinsicWidth((int) (24 * density));  // Установите нужную ширину

        transferCircle.setBackground(shapeDrawable);
        transferCircle.setGravity(Gravity.CENTER);
        TypedValue tvOnPrimary = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tvOnPrimary, true);
        transferCircle.setTextColor(tvOnPrimary.data);
        transferCircle.setTextSize(11);
        transferCircle.setPadding(3, 3, 3, 3);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(2, 0, 2, 10);
        Typeface customTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        transferCircle.setTypeface(customTypeface, Typeface.BOLD);
        transferCircle.setLayoutParams(params);

        // Добавляем обработчик клика
        transferCircle.setOnClickListener(v -> {
            // Находим линию, к которой принадлежит станция перехода
            Line transferLine = findLineForStation(transferStation);
            if (transferLine != null) {
                // Находим предыдущую и следующую станции на линии
                Station prevTransferStation = findPrevStation(transferLine, transferStation);
                Station nextTransferStation = findNextStation(transferLine, transferStation);

                // Обновляем информацию в фрагменте
                updateStationInfo(transferStation, transferLine, prevTransferStation, nextTransferStation, transfers);
            }
        });

        return transferCircle;
    }

    private Line findLineForStation(Station station) {
        if (station == null || station.getId() == null) {
            return null;
        }
        if (lines != null) {
            for (Line candidateLine : lines) {
                if (candidateLine != null && candidateLine.getStations() != null) {
                    for (Station candidateStation : candidateLine.getStations()) {
                        if (candidateStation != null && station.getId().equals(candidateStation.getId())) {
                            return candidateLine;
                        }
                    }
                }
            }
        }
        if (grayedLines != null) {
            for (Line candidateLine : grayedLines) {
                if (candidateLine != null && candidateLine.getStations() != null) {
                    for (Station candidateStation : candidateLine.getStations()) {
                        if (candidateStation != null && station.getId().equals(candidateStation.getId())) {
                            return candidateLine;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Station findPrevStation(Line line, Station currentStation) {
        List<Station> sortedStations = getSortedStations(line);
        return findNeighborById(sortedStations, currentStation, true);
    }

    private Station findNextStation(Line line, Station currentStation) {
        List<Station> sortedStations = getSortedStations(line);
        return findNeighborById(sortedStations, currentStation, false);
    }

    public void updateStationInfo(Station newStation, Line newLine, Station newPrevStation, Station newNextStation, List<Transfer> newTransfers) {
        this.station = newStation;
        this.line = newLine;
        this.prevStation = newPrevStation;
        this.nextStation = newNextStation;
        this.transfers = newTransfers;

        // Обновляем UI
        TextView stationName = getView().findViewById(R.id.stationName);
        stationName.setText(newStation.getName());

        if (getView() != null && newStation != null) {
            setupLinesList(getView(), newStation);
        }

        LinearLayout upperStationsContainer = getView().findViewById(R.id.upperStationsContainer);
        LinearLayout lowerStationsContainer = getView().findViewById(R.id.lowerStationsContainer);
        updateNeighborViews(upperStationsContainer, lowerStationsContainer);


//        LinearLayout transferCirclesContainer = getView().findViewById(R.id.transferCirclesContainer);
//        transferCirclesContainer.removeAllViews(); // Очищаем старые переходы
//        addTransferCircles(transferCirclesContainer); // Добавляем новые переходы

        // Обновляем расписание (если нужно)
        fetchESPSchedule(newStation);
    }

    public void updateLineNumber(Line newLine) {
        if (newLine == null || getView() == null) {
            return;
        }
        this.line = newLine;
        if (getView() != null && station != null) {
            setupLinesList(getView(), station);
        }
    }

    private Line findLineByStation(Station station) {
        if (station == null) return null;
        
        // Проверяем в основном списке линий
        if (lines != null) {
            for (Line l : lines) {
                if (l != null && l.getStations() != null) {
                    for (Station s : l.getStations()) {
                        if (s != null && s.getId() != null && s.getId().equals(station.getId())) {
                            return l;
                        }
                    }
                }
            }
        }
        
        // Проверяем в серых линиях
        if (grayedLines != null) {
            for (Line l : grayedLines) {
                if (l != null && l.getStations() != null) {
                    for (Station s : l.getStations()) {
                        if (s != null && s.getId() != null && s.getId().equals(station.getId())) {
                            return l;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private List<Line> findAllLinesForStation(Station station) {
        List<Line> result = new ArrayList<>();
        if (station == null || station.getId() == null) {
            return result;
        }
        
        String stationId = station.getId();
        Set<String> seenLineIds = new LinkedHashSet<>();
        
        if (lines != null) {
            for (Line l : lines) {
                if (l != null && l.getStations() != null && l.getId() != null) {
                    if (seenLineIds.contains(l.getId())) {
                        continue;
                    }
                    for (Station s : l.getStations()) {
                        if (s != null && s.getId() != null && s.getId().equals(stationId)) {
                            result.add(l);
                            seenLineIds.add(l.getId());
                            break;
                        }
                    }
                }
            }
        }
        
        if (grayedLines != null) {
            for (Line l : grayedLines) {
                if (l != null && l.getStations() != null && l.getId() != null) {
                    if (seenLineIds.contains(l.getId())) {
                        continue;
                    }
                    for (Station s : l.getStations()) {
                        if (s != null && s.getId() != null && s.getId().equals(stationId)) {
                            result.add(l);
                            seenLineIds.add(l.getId());
                            break;
                        }
                    }
                }
            }
        }
        
        return result;
    }

    private String getLineIdForStation(Station station) {
        if (lines != null) {
            for (Line line : lines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    return line.getdisplayNumber();
                }
            }
        }
        if (grayedLines != null) {
            for (Line line : grayedLines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    return line.getdisplayNumber();
                }
            }
        }
        return null;
    }

    private String buildStationKey(Station station) {
        if (station == null) {
            return "";
        }
        String id = station.getId();
        if (id != null && !id.trim().isEmpty()) {
            return id.trim();
        }
        String name = station.getName();
        return name != null ? name.trim() : "";
    }

    private int parseColorOrDefault(String color, int fallback) {
        if (color == null) {
            return fallback;
        }
        String trimmed = color.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return fallback;
        }
        try {
            return Color.parseColor(trimmed);
        } catch (IllegalArgumentException ex) {
            Log.w("StationInfoFragment", "Invalid color string: " + trimmed, ex);
            return fallback;
        }
    }

    private Shape getLineShapeForStation(Station station) {
        if (lines != null) {
            for (Line line : lines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    String displayShape = line.getDisplayShape();
                    if (displayShape != null && displayShape.equals("SQUARE")) {
                        return new RectShape();
                    } else if (displayShape != null && displayShape.equals("CIRCLE")) {
                        return new CircleShape(parseColorOrDefault(line.getColor(), Color.BLACK));
                    } else if (displayShape != null && displayShape.equals("DOUBLE_CIRCLE")) {
                        return new CircleShape(parseColorOrDefault(line.getColor(), Color.BLACK));
                    } else if (displayShape != null && displayShape.equals("PARALLELOGRAM")) {
                        return new ParallelogramShape(parseColorOrDefault(line.getColor(), Color.BLACK));
                    } else if (displayShape != null && displayShape.equals("MTD")) {
                        return new ParallelogramShape(parseColorOrDefault(line.getColor(), Color.BLACK));
                    }
                }
            }
        }
        if (grayedLines != null) {
            for (Line line : grayedLines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    String displayShape = line.getDisplayShape();
                    if (displayShape != null && displayShape.equals("SQUARE")) {
                        return new RectShape();
                    }
                }
            }
        }
        return null;
    }

    private String getColorForStation(Station station) {
        if (lines != null) {
            for (Line line : lines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    return line.getColor();
                }
            }
        }
        if (grayedLines != null) {
            for (Line line : grayedLines) {
                if (line != null && line.getStations() != null && line.getStations().contains(station)) {
                    return line.getColor();
                }
            }
        }
        return "#000000";
    }

    private View createLineIndicatorView(Line line, Station stationForLine) {
        if (line == null || getContext() == null) {
            return null;
        }

        TextView lineIndicator = new TextView(getContext());
        
        String displayNumber = line.getLineDisplayNumberForStation(stationForLine);
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = line.getdisplayNumber();
        }
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = line.getId();
        }
        lineIndicator.setText(displayNumber != null ? displayNumber : "");

        int textColor = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnBackground, Color.BLACK);
        lineIndicator.setTextColor(textColor);
        
        Typeface customTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        if (customTypeface != null) {
            lineIndicator.setTypeface(customTypeface, Typeface.BOLD);
        } else {
            lineIndicator.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        
        lineIndicator.setTextSize(16);
        lineIndicator.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0, (int) (4 * getResources().getDisplayMetrics().density));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lineIndicator.setLayoutParams(params);
        
        lineIndicator.setOnClickListener(v -> {
            selectedLineForStation = line;
            updateStationInfoForLine(line);
        });
        
        return lineIndicator;
    }

    private LinearLayout createLineColorStripLayer(Line line, Station stationForLine, int layerIndex, int totalLayers) {
        if (line == null || getContext() == null) {
            return null;
        }

        LinearLayout layer = new LinearLayout(getContext());
        layer.setOrientation(LinearLayout.VERTICAL);
        layer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        layer.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.linestrip_background));
        
        int lineColor = parseColorOrDefault(stationForLine.getColor(), Color.BLACK);
        layer.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        float density = getResources().getDisplayMetrics().density;
        
        boolean isSelected = (selectedLineForStation != null && selectedLineForStation.getId().equals(line.getId())) ||
                             (selectedLineForStation == null && line == null && layerIndex == 0) ||
                             (selectedLineForStation == null && line != null && this.line != null && this.line.getId().equals(line.getId()));
        
        if (isSelected) {
            layer.setPadding((int) (2 * density), (int) (2 * density), (int) (2 * density), (int) (2 * density));
        }
        float textSizeSp = 16f;
        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, getResources().getDisplayMetrics());
        float textPaddingTop = 4 * density;
        float textPaddingBottom = 4 * density;
        float textHeight = textSizePx + textPaddingTop + textPaddingBottom;
        float offsetDp = (textHeight / density) + 8f;
        int offsetPx = (int) (offsetDp * density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.topMargin = offsetPx * layerIndex;
        params.bottomMargin = offsetPx * (totalLayers - layerIndex - 1);
        layer.setLayoutParams(params);

        TextView lineNumberView = new TextView(getContext());
        String displayNumber = line.getLineDisplayNumberForStation(stationForLine);
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = line.getdisplayNumber();
        }
        if (displayNumber == null || displayNumber.isEmpty()) {
            displayNumber = line.getId();
        }
        lineNumberView.setText(displayNumber != null ? displayNumber : "");

        int textColor = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnBackground, Color.BLACK);
        lineNumberView.setTextColor(textColor);

        Typeface customTypeface = ResourcesCompat.getFont(getContext(), R.font.emyslabaltblack);
        if (customTypeface != null) {
            lineNumberView.setTypeface(customTypeface, Typeface.BOLD);
        } else {
            lineNumberView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        lineNumberView.setTextSize(16);
        lineNumberView.setPadding(0, (int) (4 * density), 0, (int) (4 * density));
        lineNumberView.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lineNumberView.setLayoutParams(textParams);

        lineNumberView.setOnClickListener(v -> {
            selectedLineForStation = line;
            updateStationInfoForLine(line);
        });

        layer.addView(lineNumberView);

        return layer;
    }

    private void setupLinesList(View view, Station station) {
        if (view == null || station == null) {
            return;
        }

        FrameLayout lineColorStripContainer = view.findViewById(R.id.lineColorStripContainer);
        if (lineColorStripContainer == null) {
            return;
        }

        List<Line> allLinesForStation = findAllLinesForStation(station);
        if (allLinesForStation == null || allLinesForStation.isEmpty()) {
            lineColorStripContainer.removeAllViews();
            return;
        }

        lineColorStripContainer.removeAllViews();

        int totalLayers = allLinesForStation.size();
        for (int i = 0; i < totalLayers; i++) {
            Line line = allLinesForStation.get(i);
            Station lineStation = findStationInLine(station, line);
            if (lineStation == null) {
                lineStation = station;
            }
            
            LinearLayout layer = createLineColorStripLayer(line, lineStation, i, totalLayers);
            if (layer != null) {
                lineColorStripContainer.addView(layer);
            }
        }

        if (line == null && !allLinesForStation.isEmpty()) {
            selectedLineForStation = allLinesForStation.get(0);
            line = selectedLineForStation;
        } else {
            selectedLineForStation = line;
        }
    }

    private Station findStationInLine(Station station, Line line) {
        if (station == null || line == null || station.getId() == null || line.getStations() == null) {
            return null;
        }
        String stationId = station.getId();
        for (Station lineStation : line.getStations()) {
            if (lineStation != null && lineStation.getId() != null && lineStation.getId().equals(stationId)) {
                return lineStation;
            }
        }
        return null;
    }

    private void updateStationInfoForLine(Line selectedLine) {
        if (selectedLine == null || station == null || getView() == null) {
            return;
        }

        Station stationForLine = findStationInLine(station, selectedLine);
        if (stationForLine == null) {
            stationForLine = station;
        }

        this.line = selectedLine;
        this.selectedLineForStation = selectedLine;

        Station prev = findPrevStation(selectedLine, stationForLine);
        Station next = findNextStation(selectedLine, stationForLine);
        this.prevStation = prev;
        this.nextStation = next;

        if (getView() != null && stationForLine != null) {
            setupLinesList(getView(), stationForLine);
        }

        updateNeighborViews(
                getView().findViewById(R.id.upperStationsContainer),
                getView().findViewById(R.id.lowerStationsContainer)
        );
    }

    public void setOnStationInfoListener(OnStationInfoListener listener) {
        this.listener = listener;
    }

    private void dismiss() {
        if (listener != null) {
            listener.onDismiss();
        }
    }

    public interface OnStationInfoListener {
        void onSetStart(Station station, Line line, boolean fromStationInfoFragment);
        void onSetEnd(Station station, Line line, boolean fromStationInfoFragment);
        void onDismiss();
    }
}