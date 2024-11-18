package com.nicorp.nimetro.presentation.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.presentation.adapters.FullScheduleAdapter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FullScheduleDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private List<YandexRaspResponse.Segment> segments;
    private Button refreshButton;
    private ImageView closeButton;
    private Button openCalendarButton;
    private CheckBox filterNearest;
    private CheckBox filterNoExpress;
    private RecyclerView fullScheduleRecyclerView;

    private String from;
    private String to;
    private String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";

    public static FullScheduleDialogFragment newInstance(List<YandexRaspResponse.Segment> segments, String from, String to) {
        FullScheduleDialogFragment fragment = new FullScheduleDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("segments", new ArrayList<>(segments));
        args.putString("from", from);
        args.putString("to", to);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            segments = getArguments().getParcelableArrayList("segments");
            from = getArguments().getString("from");
            to = getArguments().getString("to");
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_full_schedule, container, false);

        refreshButton = view.findViewById(R.id.refreshButton);
        closeButton = view.findViewById(R.id.closeButton);
        openCalendarButton = view.findViewById(R.id.openCalendarButton);
        filterNearest = view.findViewById(R.id.filterNearest);
        filterNoExpress = view.findViewById(R.id.filterNoExpress);
        fullScheduleRecyclerView = view.findViewById(R.id.fullScheduleRecyclerView);

        fullScheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fullScheduleRecyclerView.setAdapter(new FullScheduleAdapter(segments));

        openCalendarButton.setOnClickListener(v -> openDatePickerDialog());

        refreshButton.setOnClickListener(v -> {
            // Здесь можно добавить логику для обновления расписания с учетом выбранной даты
            fetchESPDepartureTime(from, to, getSelectedDate());
        });

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    private void openDatePickerDialog() {
        DatePickerDialogFragment datePickerDialogFragment = DatePickerDialogFragment.newInstance(this);
        datePickerDialogFragment.show(getChildFragmentManager(), "datePickerDialog");
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // Обновляем расписание с учетом выбранной даты
        fetchESPDepartureTime(from, to, String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth));
    }

    private String getSelectedDate() {
        // Здесь можно добавить логику для получения выбранной даты
        return "2024-11-18"; // Пример даты
    }

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();

    private void fetchESPDepartureTime(String from, String to, String date) {
        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, from, to, "esr", date, 1000);

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                    if (!segments.isEmpty()) {
                        segments = applyFilters(segments);
                        FullScheduleAdapter adapter = new FullScheduleAdapter(segments);
                        fullScheduleRecyclerView.setAdapter(adapter);
                    } else {
                        Log.d("FullScheduleDialog", "No segments found for ESP departure time");
                    }
                } else {
                    Log.e("FullScheduleDialog", "Failed to fetch ESP departure time: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.e("FullScheduleDialog", "Error fetching ESP departure time", t);
            }
        });
    }

    private List<YandexRaspResponse.Segment> applyFilters(List<YandexRaspResponse.Segment> segments) {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        return segments.stream()
                .filter(segment -> {
                    if (filterNearest.isChecked()) {
                        try {
                            ZonedDateTime departureDateTime = ZonedDateTime.parse(segment.getDeparture(), formatter);
                            return departureDateTime.isAfter(currentTime);
                        } catch (Exception e) {
                            Log.e("FullScheduleDialog", "Error parsing date", e);
                            return false;
                        }
                    }
                    return true;
                })
                .filter(segment -> {
                    if (filterNoExpress.isChecked()) {
                        return segment.getThread().getExpressType() == null;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}