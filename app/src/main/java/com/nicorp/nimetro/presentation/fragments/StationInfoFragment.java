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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

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
import java.util.List;
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

        // Находим TextView для предыдущей и следующей станции
        TextView prevStationName = view.findViewById(R.id.prevStationName);
        TextView nextStationName = view.findViewById(R.id.nextStationName);

        // Устанавливаем видимость и иконки в зависимости от isCircle
        if (line.isCircle()) {
            // Если линия круговая, используем стрелки по часовой и против часовой
            setStationNameDrawable(prevStationName, R.drawable.ic_clockwise);
            setStationNameDrawable(nextStationName, R.drawable.ic_counter_clockwise);
        } else {
            // Если линия не круговая, используем стрелки вверх и вниз
            setStationNameDrawable(prevStationName, R.drawable.ic_arrow_up);
            setStationNameDrawable(nextStationName, R.drawable.ic_arrow_down);
        }

        // Остальная логика
        TextView stationName = view.findViewById(R.id.stationName);
        stationName.setText(station.getName());
        Log.d("StationInfoFragmentInfo", "Station name: " + station.getName());

        // Если станция конечная, отображаем только одну станцию (prev или next)
        if (prevStation == null || nextStation == null) {
            if (prevStation != null) {
                // Если есть только предыдущая станция, отображаем ее в nextStationName
                setStationNameVisibility(nextStationName, prevStation);
                prevStationName.setVisibility(View.INVISIBLE); // Используем INVISIBLE вместо GONE
            } else if (nextStation != null) {
                // Если есть только следующая станция, отображаем ее в prevStationName
                setStationNameVisibility(prevStationName, nextStation);
                nextStationName.setVisibility(View.INVISIBLE); // Используем INVISIBLE вместо GONE
            }
        } else {
            // Если есть обе станции, отображаем их как обычно
            setStationNameVisibility(prevStationName, prevStation);
            setStationNameVisibility(nextStationName, nextStation);
        }

        TextView fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> onFromButtonClick());

        TextView toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> onToButtonClick());

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        TextView lineNumber = view.findViewById(R.id.lineNumber);
        View lineColorStrip = view.findViewById(R.id.lineColorStrip);
        setLineNumberAndColor(lineNumber, lineColorStrip, station);

        // Вызов fetchESPSchedule
        fetchESPSchedule(station);

        return view;
    }

    /**
     * Устанавливает drawableStart для TextView
     *
     * @param textView TextView, для которого нужно установить иконку
     * @param drawableResId ID ресурса иконки
     */
    private void setStationNameDrawable(TextView textView, int drawableResId) {
        if (textView != null) {
            textView.setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0);
        }
    }

    private void fetchESPSchedule(Station station) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
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

    private void setLineNumberAndColor(TextView lineNumber, View lineColorStrip, Station station) {
        String lineId = String.valueOf(line.getLineDisplayNumberForStation(station));
        lineNumber.setText(lineId);

        lineColorStrip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(station.getColor())));

        Shape shape = getLineShapeForStation(station);
        float density = getResources().getDisplayMetrics().density;

        // Создаем LayerDrawable для комбинации отступов и формы
        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);
        shapeDrawable.getPaint().setColor(Color.parseColor(station.getColor()));

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

        Shape shape = getLineShapeForStation(transferStation);
        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);
        shapeDrawable.getPaint().setColor(Color.parseColor(getColorForStation(transferStation)));
        float density = getResources().getDisplayMetrics().density;
        shapeDrawable.setIntrinsicHeight((int) (24 * density)); // Установите нужную высоту
        shapeDrawable.setIntrinsicWidth((int) (24 * density));  // Установите нужную ширину

        transferCircle.setBackground(shapeDrawable);
        transferCircle.setGravity(Gravity.CENTER);
        transferCircle.setTextColor(Color.WHITE);
        if (shape instanceof DoubleCircleShape) {
            transferCircle.setTextColor(Color.BLACK);
        }
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
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line;
            }
        }
        return null;
    }

    private Station findPrevStation(Line line, Station currentStation) {
        List<Station> stations = line.getStations();
        int index = stations.indexOf(currentStation);
        if (index > 0) {
            return stations.get(index - 1);
        }
        return null;
    }

    private Station findNextStation(Line line, Station currentStation) {
        List<Station> stations = line.getStations();
        int index = stations.indexOf(currentStation);
        if (index < stations.size() - 1) {
            return stations.get(index + 1);
        }
        return null;
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

        TextView prevStationName = getView().findViewById(R.id.prevStationName);
        setStationNameVisibility(prevStationName, newPrevStation);

        TextView nextStationName = getView().findViewById(R.id.nextStationName);
        setStationNameVisibility(nextStationName, newNextStation);

        TextView lineNumber = getView().findViewById(R.id.lineNumber);
        View lineColorStrip = getView().findViewById(R.id.lineColorStrip);
        setLineNumberAndColor(lineNumber, lineColorStrip, newStation);

//        LinearLayout transferCirclesContainer = getView().findViewById(R.id.transferCirclesContainer);
//        transferCirclesContainer.removeAllViews(); // Очищаем старые переходы
//        addTransferCircles(transferCirclesContainer); // Добавляем новые переходы

        // Обновляем расписание (если нужно)
        fetchESPSchedule(newStation);
    }

    private String getLineIdForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line.getdisplayNumber();
            }
        }
        for (Line line : grayedLines) {
            if (line.getStations().contains(station)) {
                return line.getdisplayNumber();
            }
        }
        return null;
    }

    private Shape getLineShapeForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                if (line.getDisplayShape().equals("SQUARE")) {
                    return new RectShape();
                } else if (line.getDisplayShape().equals("CIRCLE")) {
                    return new CircleShape(Color.parseColor(line.getColor()));
                } else if (line.getDisplayShape().equals("DOUBLE_CIRCLE")) {
                    return new CircleShape(Color.parseColor(line.getColor()));
                } else if (line.getDisplayShape().equals("PARALLELOGRAM")) {
                    return new ParallelogramShape(Color.parseColor(line.getColor()));
                }
            }
        }
        for (Line line : grayedLines) {
            if (line.getStations().contains(station)) {
                if (line.getDisplayShape().equals("SQUARE")) {
                    return new RectShape();
                }
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
        if (listener != null) {
            listener.onDismiss();
        }
    }

    public interface OnStationInfoListener {
        void onSetStart(Station station, boolean fromStationInfoFragment);
        void onSetEnd(Station station, boolean fromStationInfoFragment);
        void onDismiss(); // Добавляем новый метод для закрытия ViewPager2
    }
}