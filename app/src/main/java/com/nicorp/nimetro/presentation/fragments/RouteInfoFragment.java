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

import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.api.YandexRaspApi;
import com.nicorp.nimetro.data.models.YandexRaspResponse;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.views.MetroMapView;

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

/**
 * A fragment that displays detailed information about a metro route.
 * This includes the route's total time, number of stations, number of transfers,
 * and a detailed list of stations with transfer indicators.
 */
public class RouteInfoFragment extends Fragment {

    private static final String ARG_ROUTE = "route";
    private static final int COLLAPSED_HEIGHT = 178;

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

    /**
     * Creates a new instance of RouteInfoFragment with the necessary arguments.
     *
     * @param route The route to display information about.
     * @param metroMapView The MetroMapView instance.
     * @param mainActivity The MainActivity instance.
     * @return A new instance of RouteInfoFragment.
     */
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

        // Вызов fetchESPDepartureTime
        if (route != null && !route.isEmpty()) {
            fetchESPDepartureTime(route.get(0));
        }

        return view;
    }

    /**
     * Initializes the views used in the fragment.
     *
     * @param view The root view of the fragment.
     * @param colorOnSurface The color to use for text views.
     */
    private void initializeViews(View view, int colorOnSurface) {
        nearestTrainsTextView = view.findViewById(R.id.nearestTrains);
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

    /**
     * Sets the text colors for the views.
     *
     * @param colorOnSurface The color to use for text views.
     */
    private void setViewColors(int colorOnSurface) {
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);
    }

    /**
     * Calculates and sets the route statistics (total time, number of stations, number of transfers).
     */
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

    /**
     * Sets up the close button to dismiss the fragment.
     *
     * @param view The root view of the fragment.
     */
    private void setupCloseButton(View view) {
        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
    }

    /**
     * Sets up the swipe gesture detector to handle expand and collapse actions.
     *
     * @param view The root view of the fragment.
     */
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

    /**
     * Dismisses the fragment and clears the route and selected stations on the map.
     */
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

    /**
     * Expands the fragment to show detailed route information.
     */
    private void expand() {
        isExpanded = true;
        routeTitle.setText("Информация о маршруте");
        layoutCollapsed.setVisibility(View.GONE);
        layoutExpanded.setVisibility(View.VISIBLE);

        float expandedHeight = getExpandedHeight();
        animateHeightChange(routeInfoContainer, COLLAPSED_HEIGHT, expandedHeight);
    }

    /**
     * Collapses the fragment to show only summary route information.
     */
    private void collapse() {
        isExpanded = false;
        routeTitle.setText("Краткая информация");
        layoutCollapsed.setVisibility(View.VISIBLE);
        layoutExpanded.setVisibility(View.GONE);

        float density = getResources().getDisplayMetrics().density;
        animateHeightChange(routeInfoContainer, routeInfoContainer.getHeight(), COLLAPSED_HEIGHT * density);
    }

    /**
     * Animates the height change of the view.
     *
     * @param view The view to animate.
     * @param startHeight The starting height of the view.
     * @param endHeight The ending height of the view.
     */
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

    /**
     * Calculates the total time for the route.
     *
     * @return The total time in minutes.
     */
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

    /**
     * Calculates the number of transfers in the route.
     *
     * @return The number of transfers.
     */
    private int calculateTransfersCount() {
        int transfers = 0;
        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                transfers++;
            }
        }
        return transfers;
    }

    /**
     * Populates the route details container with station and transfer information.
     *
     * @param container The container to populate.
     */
    private void populateRouteDetails(LinearLayout container) {
        container.post(() -> {
            container.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (int i = 0; i < route.size(); i++) {
                Station station = route.get(i);

                View stationView = inflater.inflate(R.layout.item_route_station, container, false);
                TextView stationName = stationView.findViewById(R.id.stationName);
                View stationIndicator = stationView.findViewById(R.id.stationIndicator);

                stationName.setText(station.getName());
                stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

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

    /**
     * Gets the expanded height of the layout.
     *
     * @return The expanded height in pixels.
     */
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

    private void fetchESPDepartureTime(Station startStation) {
        String apiKey = "e4d3d8fe-a921-4206-8048-8c7217648728";
        String from = startStation.getESP();
        String to = route.get(1).getESP();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Log.d("RouteInfoFragment", "Fetching ESP departure time for station: " + startStation.getName() + " from: " + from + " to: " + to + " on date: " + date);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.rasp.yandex.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YandexRaspApi yandexRaspApi = retrofit.create(YandexRaspApi.class);
        Call<YandexRaspResponse> call = yandexRaspApi.getSchedule("ru_RU", "json", apiKey, from, to, "esr", date);

        call.enqueue(new Callback<YandexRaspResponse>() {
            @Override
            public void onResponse(Call<YandexRaspResponse> call, Response<YandexRaspResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YandexRaspResponse.Segment> segments = response.body().getSegments();
                    if (!segments.isEmpty()) {
                        List<String> nearestDepartureTimes = findNearestDepartureTimes(segments);
                        Log.d("RouteInfoFragment", "Nearest ESP departure times: " + nearestDepartureTimes);
                        // Отобразить время отправления в UI
                        showESPDepartureTimes(nearestDepartureTimes);
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

    private List<String> findNearestDepartureTimes(List<YandexRaspResponse.Segment> segments) {
        List<String> nearestDepartureTimes = new ArrayList<>();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        for (YandexRaspResponse.Segment segment : segments) {
            try {
                Log.d("RouteInfoFragment", "Departure: " + segment.getDeparture());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                ZonedDateTime departureDateTime = ZonedDateTime.parse(segment.getDeparture(), formatter);
                long difference = ChronoUnit.MINUTES.between(currentTime, departureDateTime);

                if (difference > 0 && nearestDepartureTimes.size() < 3) {
                    nearestDepartureTimes.add(segment.getDeparture().substring(11, 16));
                }
            } catch (Exception e) {
                Log.e("RouteInfoFragment", "Error parsing date", e);
            }
        }

        return nearestDepartureTimes;
    }

    private void showESPDepartureTimes(List<String> departureTimes) {
        nearestTrainsTextView.setText("Ближайшие электрички: " + String.join(", ", departureTimes));
    }
}