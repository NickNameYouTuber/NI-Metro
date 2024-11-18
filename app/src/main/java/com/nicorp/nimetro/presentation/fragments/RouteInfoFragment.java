package com.nicorp.nimetro.presentation.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.adapters.TrainInfoAdapter;
import com.nicorp.nimetro.presentation.views.MetroMapView;

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

public class RouteInfoFragment extends Fragment {

    private static final String ARG_ROUTE = "route";
    private static final int COLLAPSED_HEIGHT = 308;

    private List<Station> route;
    private boolean isExpanded = false;

    private TextView routeTime;
    private TextView nearestTrainsTextView;
    private TextView routeStationsCount;
    private TextView routeTransfersCount;
    private TextView routeTitle;
    private TextView routeTimeTitle;
    private TextView routeStationsCountTitle;
    private TextView routeTransfersCountTitle;
    private LinearLayout routeDetailsContainer;
    private LinearLayout layoutCollapsed;
    private LinearLayout layoutExpanded;
    private FrameLayout routeInfoContainer;
    private MetroMapView metroMapView;
    private MainActivity mainActivity;
    private RecyclerView nearestTrainsRecyclerView;

    public static RouteInfoFragment newInstance(List<Station> route, MetroMapView metroMapView, MainActivity mainActivity) {
        RouteInfoFragment fragment = new RouteInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ROUTE, new Route(route));
        fragment.metroMapView = metroMapView;
        fragment.mainActivity = mainActivity;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Route routeParcelable = getArguments().getParcelable(ARG_ROUTE);
            if (routeParcelable != null) {
                route = routeParcelable.getStations();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_route_info, container, false);

        int colorOnSurface = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

        initializeViews(view, colorOnSurface);
        setViewColors(colorOnSurface);
        calculateAndSetRouteStatistics();
        setupCloseButton(view);
        setupSwipeGestureDetector(view);

        nearestTrainsRecyclerView = view.findViewById(R.id.nearestTrainsRecyclerView);
        nearestTrainsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        if (route != null && !route.isEmpty()) {
            determineTrainInfoDisplay(route);
        }

        return view;
    }

    private void initializeViews(View view, int colorOnSurface) {
        routeTime = view.findViewById(R.id.routeTime);
        routeStationsCount = view.findViewById(R.id.routeStationsCount);
        routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        routeTitle = view.findViewById(R.id.routeTitle);
        routeTimeTitle = view.findViewById(R.id.routeTimeTitle);
        routeStationsCountTitle = view.findViewById(R.id.routeStationsTitle);
        routeTransfersCountTitle = view.findViewById(R.id.routeTransfersTitle);
        routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        layoutCollapsed = view.findViewById(R.id.layoutCollapsed);
        layoutExpanded = view.findViewById(R.id.layoutExpanded);
        routeInfoContainer = view.findViewById(R.id.routeInfoContainer);
    }

    private void setViewColors(int colorOnSurface) {
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);
    }

    private void calculateAndSetRouteStatistics() {
        if (route != null && !route.isEmpty()) {
            int totalTime = calculateTotalTime();
            int stationsCount = route.size();
            int transfersCount = calculateTransfersCount();

            routeTime.setText(String.format("%d мин", totalTime));
            routeStationsCount.setText(String.format("%d", stationsCount));
            routeTransfersCount.setText(String.format("%d", transfersCount));

            populateRouteDetails(routeDetailsContainer);
        }
    }

    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
    }

    private void setupSwipeGestureDetector(View view) {
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityY) > Math.abs(velocityX)) {
                    if (velocityY > 0) {
                        if (isExpanded) {
                            collapse();
                        }
                    } else {
                        if (!isExpanded) {
                            expand();
                        }
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        routeInfoContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void dismiss() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
            if (metroMapView != null) {
                metroMapView.clearRoute();
                metroMapView.clearSelectedStations();
            }
            if (mainActivity != null) {
                mainActivity.clearRouteInputs();
            }
        }
    }

    private void expand() {
        isExpanded = true;
        routeTitle.setText("Информация о маршруте");
        layoutCollapsed.setVisibility(View.GONE);
        layoutExpanded.setVisibility(View.VISIBLE);

        float expandedHeight = getExpandedHeight();
        animateHeightChange(routeInfoContainer, COLLAPSED_HEIGHT, expandedHeight);
    }

    private void collapse() {
        isExpanded = false;
        routeTitle.setText("Краткая информация");
        layoutCollapsed.setVisibility(View.VISIBLE);
        layoutExpanded.setVisibility(View.GONE);

        float density = getResources().getDisplayMetrics().density;
        animateHeightChange(routeInfoContainer, routeInfoContainer.getHeight(), COLLAPSED_HEIGHT * density);
    }

    private void animateHeightChange(final View view, final int startHeight, final float endHeight) {
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int newHeight = (int) (startHeight + (endHeight - startHeight) * interpolatedTime);
                view.getLayoutParams().height = newHeight;
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(300);
        view.startAnimation(animation);
    }

    private int calculateTotalTime() {
        int totalTime = 0;
        totalTime += (route.size() - 1) * 2;

        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                totalTime += 3;
            }
        }
        return totalTime;
    }

    private int calculateTransfersCount() {
        int transfers = 0;
        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                transfers++;
            }
        }
        return transfers;
    }

    private void populateRouteDetails(LinearLayout container) {
        container.post(() -> {
            container.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (int i = 0; i < route.size(); i++) {
                Station station = route.get(i);

                View stationView = inflater.inflate(R.layout.item_route_station, container, false);
                TextView stationName = stationView.findViewById(R.id.stationName);
                View stationIndicator = stationView.findViewById(R.id.stationIndicator);
                View stationIndicatorDouble = stationView.findViewById(R.id.stationIndicatorDouble);

                stationName.setText(station.getName());
                stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

                // Проверяем, является ли линия типа "double"
                if (isLineDouble(station)) {
                    stationIndicatorDouble.setVisibility(View.VISIBLE);
                    stationIndicatorDouble.setBackgroundColor(Color.parseColor(station.getColor()));
                }

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 0);
                stationView.setLayoutParams(params);

                if (i == 0 || i == route.size() - 1) {
                    stationName.setTypeface(null, Typeface.BOLD);
                }

                container.addView(stationView);

                if (i < route.size() - 1 && !route.get(i + 1).getColor().equals(station.getColor())) {
                    View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                    LinearLayout.LayoutParams transferParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    transferParams.setMargins(0, 8, 0, 8);
                    transferView.setLayoutParams(transferParams);
                    container.addView(transferView);
                }
            }

            container.requestLayout();
        });
    }

    private boolean isLineDouble(Station station) {
        for (Line line : mainActivity.getAllLines()) {
            if (line.getStations().contains(station) && "double".equals(line.getLineType())) {
                return true;
            }
        }
        return false;
    }

    private float getExpandedHeight() {
        layoutExpanded.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float expandedHeight = layoutExpanded.getMeasuredHeight();
        float density = getResources().getDisplayMetrics().density;
        Log.d("RouteInfoFragment", "Expanded height: " + expandedHeight);
        if (expandedHeight > 500 * density) {
            expandedHeight = 500 * density;
        }
        return expandedHeight;
    }

    private void determineTrainInfoDisplay(List<Station> route) {
        Station suburbanStartStation = null;
        Station suburbanEndStation = null;
        int count = 0;

        // Поиск первой suburban линии и ее границ
        for (int i = 0; i < route.size(); i++) {
            // Найти начальную станцию suburban линии
            if (isLineSuburban(route.get(i)) && suburbanStartStation == null) {
                suburbanStartStation = route.get(i);
            }

            if (isLineSuburban(route.get(i))){
                count++;
            }

            // Найти конечную станцию suburban линии
            if (suburbanStartStation != null && (!isLineSuburban(route.get(i)) || i == route.size() - 1)) {
                suburbanEndStation = isLineSuburban(route.get(i)) ? route.get(i) : route.get(i - 1);
                break;
            }
        }


        // Если найдены suburban станции
        if (suburbanStartStation != null && suburbanEndStation != null) {
            Log.d("RouteInfoFragment", "Suburban route detected: from " +
                    suburbanStartStation.getName() + " to " + suburbanEndStation.getName());
            Log.d("RouteInfoFragment", "Count: " + count + " size: " + route.size());
            if (count == route.size()) {
                fetchESPDepartureTime(suburbanStartStation, suburbanEndStation);
            } else {
                fetchAndDisplaySuburbanSchedule(suburbanStartStation, suburbanEndStation);
            }
        } else if (suburbanStartStation != null) {
            Log.d("RouteInfoFragment", "suburbanStartStation " + suburbanStartStation.getName());
        } else if (suburbanEndStation != null) {
            Log.d("RouteInfoFragment", "suburbanEndStation " + suburbanEndStation.getName());
        } else {
            Log.d("RouteInfoFragment", "No suburban stations found");
        }
    }

    // Запрос расписания поездов
    private void fetchAndDisplaySuburbanSchedule(Station startStation, Station endStation) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        String from = startStation.getESP();
        String to = endStation.getESP();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("RouteInfoFragment", "Fetching suburban schedule: from " + startStation.getName() +
                " to " + endStation.getName() + " on " + date);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

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
                        List<YandexRaspResponse.Segment> nearestSegments = findNearestDepartureTimes(segments);
                        displayNearestTrains(startStation, nearestSegments);
                    } else {
                        Log.d("RouteInfoFragment", "No segments found for suburban schedule");
                    }
                } else {
                    Log.e("RouteInfoFragment", "Failed to fetch suburban schedule: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.e("RouteInfoFragment", "Error fetching suburban schedule", t);
            }
        });
    }

    // Отображение поездов на первой suburban станции
    private void displayNearestTrains(Station startStation, List<YandexRaspResponse.Segment> segments) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        routeDetailsContainer.post(() -> {
            for (int i = 0; i < routeDetailsContainer.getChildCount(); i++) {
                View stationView = routeDetailsContainer.getChildAt(i);
                TextView stationName = stationView.findViewById(R.id.stationName);

                if (stationName != null && stationName.getText().toString().equals(startStation.getName())) {
                    RecyclerView nearestTrainsRV = stationView.findViewById(R.id.nearestTrainsRV);
                    if (nearestTrainsRV != null) {
                        nearestTrainsRV.setVisibility(View.VISIBLE);
                        nearestTrainsRV.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                        setupTrainInfoAdapter(segments, nearestTrainsRV);
                    }
                    break;
                }
            }
        });
    }

    private boolean isLineSuburban(Station station) {
        for (Line line : mainActivity.getAllLines()) {
            if (line.getStations().contains(station) && "suburban".equals(line.getLineType())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSuburbanLineInRoute(List<Station> route) {
        for (Station station : route) {
            if (isLineSuburban(station)) {
                return true;
            }
        }
        return false;
    }

    private void fetchESPDepartureTime(Station startStation, Station endStstion) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        String from = startStation.getESP();
        String to = endStstion.getESP();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("RouteInfoFragment", "Fetching ESP departure time for station: " + startStation.getName() + " from: " + from + " to: " + to + " on date: " + date);

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
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, from, to, "esr", date, 1000);

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                    if (!segments.isEmpty()) {
                        List<YandexRaspResponse.Segment> nearestDepartureSegments = findNearestDepartureTimes(segments);
                        setupTrainInfoAdapter(nearestDepartureSegments, nearestTrainsRecyclerView);
                    } else {
                        Log.d("RouteInfoFragment", "No segments found for ESP departure time");
                    }
                } else {
                    Log.e("RouteInfoFragment", "Failed to fetch ESP departure time: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.e("RouteInfoFragment", "Error fetching ESP departure time", t);
            }
        });
    }

    private List<YandexRaspResponse.Segment> findNearestDepartureTimes(List<YandexRaspResponse.Segment> segments) {
        List<YandexRaspResponse.Segment> nearestDepartureTimes = new ArrayList<>();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        for (YandexRaspResponse.Segment segment : segments) {
            try {
                Log.d("RouteInfoFragment", "Departure: " + segment.getDeparture());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                ZonedDateTime departureDateTime = ZonedDateTime.parse(segment.getDeparture(), formatter);
                long difference = ChronoUnit.MINUTES.between(currentTime, departureDateTime);

                if (difference > 0 && nearestDepartureTimes.size() < 3) {
                    nearestDepartureTimes.add(segment);
                }
            } catch (Exception e) {
                Log.e("RouteInfoFragment", "Error parsing date", e);
            }
        }

        return nearestDepartureTimes;
    }

    private void showTrainInfoInRouteDetails(List<Station> route) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < route.size(); i++) {
            Station station = route.get(i);
            if (isLineSuburban(station)) {
                TextView stationName = getView().findViewById(R.id.stationName);
                View stationIndicator = getView().findViewById(R.id.stationIndicator);

                stationName.setText(station.getName());
                stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 0);
                getView().setLayoutParams(params);

                if (i == 0 || i == route.size() - 1) {
                    stationName.setTypeface(null, Typeface.BOLD);
                }

                routeDetailsContainer.addView(getView());

                TextView nearestTrainsTitle = getView().findViewById(R.id.nearestTrainsTitle);
                RecyclerView nearestTrainsRV = getView().findViewById(R.id.nearestTrainsRV);

                nearestTrainsTitle.setVisibility(View.VISIBLE);
                nearestTrainsRV.setVisibility(View.VISIBLE);

                // Устанавливаем LayoutManager для nearestTrainsRV
                nearestTrainsRV.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

                fetchESPDepartureTime(station, nearestTrainsRV);

                break;
            }
        }
    }

    private void fetchESPDepartureTime(Station startStation, RecyclerView recyclerView) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        String from = startStation.getESP();
        String to = route.get(route.size()-1).getESP();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("RouteInfoFragment", "Fetching ESP departure time for station: " + startStation.getName() + " from: " + from + " to: " + to + " on date: " + date);

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
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, from, to, "esr", date, 1000);

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                    if (!segments.isEmpty()) {
                        List<YandexRaspResponse.Segment> nearestDepartureSegments = findNearestDepartureTimes(segments);
                        setupTrainInfoAdapter(nearestDepartureSegments, recyclerView);
                    } else {
                        Log.d("RouteInfoFragment", "No segments found for ESP departure time");
                    }
                } else {
                    Log.e("RouteInfoFragment", "Failed to fetch ESP departure time: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<YandexRaspResponse> call, Throwable t) {
                Log.e("RouteInfoFragment", "Error fetching ESP departure time", t);
            }
        });
    }

    private void setupTrainInfoAdapter(List<YandexRaspResponse.Segment> segments, RecyclerView recyclerView) {
        Log.d("RouteInfoFragment", "Setting up train info adapter with: " + segments.size() + " segments");
        Log.d("RouteInfoFragment", "RecyclerView: " + recyclerView.toString());
        TrainInfoAdapter adapter = new TrainInfoAdapter(segments, () -> {
            FullScheduleDialogFragment dialogFragment = FullScheduleDialogFragment.newInstance(segments, route.get(0).getESP(), route.get(route.size() - 1).getESP());
            dialogFragment.show(getChildFragmentManager(), "FullScheduleDialogFragment");
        });
        recyclerView.setAdapter(adapter);
        recyclerView.requestLayout();
        recyclerView.invalidate();
    }
}