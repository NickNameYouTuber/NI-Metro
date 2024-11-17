package com.nicorp.nimetro.presentation.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        view.setMinimumWidth(LinearLayout.LayoutParams.MATCH_PARENT);

        TextView stationName = view.findViewById(R.id.stationName);
        stationName.setText(station.getName());
        Log.d("StationInfoFragmentInfo", "Station name: " + station.getName());

        prevStationArrivalTime = view.findViewById(R.id.prevStationArrivalTime);
        nextStationArrivalTime = view.findViewById(R.id.nextStationArrivalTime);

        TextView prevStationName = view.findViewById(R.id.prevStationName);
        setStationNameVisibility(prevStationName, prevStation);

        TextView nextStationName = view.findViewById(R.id.nextStationName);
        setStationNameVisibility(nextStationName, nextStation);

        TextView fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> onFromButtonClick());

        TextView toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> onToButtonClick());

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        TextView lineNumber = view.findViewById(R.id.lineNumber);
        setLineNumberAndColor(lineNumber, station);

        stationInfoContainer = view.findViewById(R.id.stationInfoContainer);

        LinearLayout transferCirclesContainer = view.findViewById(R.id.transferCirclesContainer);
//        addTransferCircles(transferCirclesContainer);

        // Вызов fetchESPSchedule
        fetchESPSchedule(station);

        return view;
    }

    private void fetchESPSchedule(Station station) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("StationInfoFragment", "Fetching ESP schedule for station: " + station.getName() + " on date: " + date);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);

        // Запрос для предыдущей станции
        if (prevStation != null) {
            String fromPrev = station.getESP();
            String toPrev = prevStation.getESP();
            Call<YandexRaspResponse> callPrev = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, fromPrev, toPrev, "esr", date);
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
            Call<YandexRaspResponse> callNext = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, fromNext, toNext, "esr", date);
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
        long minDifference = Long.MAX_VALUE;
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        for (YandexRaspResponse.Segment segment : segments) {
            try {
                Log.d("StationInfoFragment", "Arrival: " + segment.getArrival());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                ZonedDateTime arrivalDateTime = ZonedDateTime.parse(segment.getArrival(), formatter);
                long difference = ChronoUnit.MINUTES.between(currentTime, arrivalDateTime);

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
            prevStationArrivalTime.setText(String.join(", ", arrivalTimePrev));
        }
        if (arrivalTimeNext != null) {
            nextStationArrivalTime.setText(String.join(", ", arrivalTimeNext));
        }
    }

    private void showESPArrivalTime(String arrivalTime) {
        TextView arrivalTimeTextView = new TextView(getContext());
        arrivalTimeTextView.setText("Прибытие электрички: " + arrivalTime);
        arrivalTimeTextView.setTextColor(Color.BLACK);
        arrivalTimeTextView.setPadding(16, 8, 16, 8);

        stationInfoContainer.addView(arrivalTimeTextView);
    }

    private void setStationNameVisibility(TextView stationNameTextView, Station station) {
        if (station != null) {
            stationNameTextView.setText(station.getName());
            stationNameTextView.setVisibility(View.VISIBLE);
        } else {
            stationNameTextView.setVisibility(View.GONE);
        }
    }

    private void onFromButtonClick() {
        if (listener != null) {
            listener.onSetStart(station, true);
        }
        dismiss();
    }

    private void onToButtonClick() {
        if (listener != null) {
            listener.onSetEnd(station, true);
        }
        dismiss();
    }

    private void setLineNumberAndColor(TextView lineNumber, Station station) {
        lineNumber.setText(String.valueOf(line.getLineIdForStation(station)));
        lineNumber.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(station.getColor())));
    }

    private void addTransferCircles(LinearLayout transferCirclesContainer) {
        for (Transfer transfer : transfers) {
            if (transfer.getStations().contains(station)) {
                for (Station transferStation : transfer.getStations()) {
                    if (!transferStation.equals(station)) {
                        Log.d("StationInfoFragment", "Transfer station: " + transferStation);

                        TextView transferCircle = createTransferCircle(transferStation);
                        transferCirclesContainer.addView(transferCircle);
                    }
                }
            }
        }
    }

    private TextView createTransferCircle(Station transferStation) {
        TextView transferCircle = new TextView(getContext());
        String transferLineId = getLineIdForStation(transferStation);
        transferCircle.setText(String.valueOf(transferLineId));
        transferCircle.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.circle_background_red));
        transferCircle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(getColorForStation(transferStation))));
        transferCircle.setGravity(Gravity.CENTER);
        transferCircle.setTextColor(Color.WHITE);
        transferCircle.setTextSize(12);
        transferCircle.setPadding(4, 4, 4, 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 8);
        transferCircle.setLayoutParams(params);
        return transferCircle;
    }

    private String getLineIdForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line.getId();
            }
        }
        for (Line line : grayedLines) {
            if (line.getStations().contains(station)) {
                return line.getId();
            }
        }
        return null;
    }

    private String getColorForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line.getColor();
            }
        }
        for (Line line : grayedLines) {
            if (line.getStations().contains(station)) {
                return line.getColor();
            }
        }
        return "#000000";
    }

    public void setOnStationInfoListener(OnStationInfoListener listener) {
        this.listener = listener;
    }

    private void dismiss() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
        }
    }

    public interface OnStationInfoListener {
        void onSetStart(Station station, boolean fromStationInfoFragment);
        void onSetEnd(Station station, boolean fromStationInfoFragment);
    }
}