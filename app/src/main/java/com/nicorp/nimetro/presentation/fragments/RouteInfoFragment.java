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
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.activities.MainActivity;
import com.nicorp.nimetro.presentation.views.MetroMapView;

import java.util.List;

public class RouteInfoFragment extends Fragment {

    private static final String ARG_ROUTE = "route";
    private static final int COLLAPSED_HEIGHT = 178;

    private List<Station> route;
    private boolean isExpanded = false;

    private TextView routeTime;
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

        // Initialize views
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

        // Set text view colors to colorOnSurface
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);

        // Calculate route statistics
        if (route != null && !route.isEmpty()) {
            int totalTime = calculateTotalTime();
            int stationsCount = route.size();
            int transfersCount = calculateTransfersCount();

            // Update summary information
            routeTime.setText(String.format("%d мин", totalTime));
            routeStationsCount.setText(String.format("%d", stationsCount));
            routeTransfersCount.setText(String.format("%d", transfersCount));

            // Populate route details
            populateRouteDetails(routeDetailsContainer);
        }

        ImageView closeButton =  view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        // Set up swipe gesture detector
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityY) > Math.abs(velocityX)) {
                    if (velocityY > 0) {
                        // Swipe down to collapse
                        if (isExpanded) {
                            collapse();
                        }
                    } else {
                        // Swipe up to expand
                        if (!isExpanded) {
                            expand();
                        }
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        routeInfoContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        return view;
    }

    private void dismiss() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
            if (metroMapView != null) {
                metroMapView.clearRoute(); // Очищаем маршрут на карте
                metroMapView.clearSelectedStations(); // Очищаем выбранные станции
            }
            if (mainActivity != null) {
                mainActivity.clearRouteInputs(); // Очищаем поля ввода
            }
        }
    }

    private void expand() {
        isExpanded = true;
        routeTitle.setText("Информация о маршруте");
        layoutCollapsed.setVisibility(View.GONE);
        layoutExpanded.setVisibility(View.VISIBLE);

        // Calculate the height of the expanded content
        layoutExpanded.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float expandedHeight = layoutExpanded.getMeasuredHeight();
        float density = getResources().getDisplayMetrics().density; // Get the density
        Log.d("RouteInfoFragment", "Expanded height: " + expandedHeight);
        if (expandedHeight > 500 * density) {
            expandedHeight = 500 * density;
        }

        // Animate the height change
        animateHeightChange(routeInfoContainer, COLLAPSED_HEIGHT, expandedHeight);
    }

    private void collapse() {
        isExpanded = false;
        routeTitle.setText("Краткая информация");
        layoutCollapsed.setVisibility(View.VISIBLE);
        layoutExpanded.setVisibility(View.GONE);

        float density = getResources().getDisplayMetrics().density; // Get the density
        // Animate the height change
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

        animation.setDuration(300); // Set the duration of the animation
        view.startAnimation(animation);
    }

    private int calculateTotalTime() {
        int totalTime = 0;
        // Base time between stations
        totalTime += (route.size() - 1) * 2; // 2 minutes between stations

        // Add transfer times
        for (int i = 1; i < route.size(); i++) {
            if (!route.get(i).getColor().equals(route.get(i - 1).getColor())) {
                totalTime += 3; // 3 minutes for transfer
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
        container.post(new Runnable() {
            @Override
            public void run() {
                container.removeAllViews();

                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (int i = 0; i < route.size(); i++) {
                    Station station = route.get(i);

                    // Add station view
                    View stationView = inflater.inflate(R.layout.item_route_station, container, false);
                    TextView stationName = stationView.findViewById(R.id.stationName);
                    View stationIndicator = stationView.findViewById(R.id.stationIndicator);

                    stationName.setText(station.getName());
                    stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 0); // Add some vertical spacing
                    stationView.setLayoutParams(params);

                    if (i == 0 || i == route.size() - 1) {
                        // Set station name bold
                        stationName.setTypeface(null, Typeface.BOLD);
                    }

                    container.addView(stationView);

                    // Add transfer indicator if needed
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

                // Force a layout pass
                container.requestLayout();
            }
        });
    }
}