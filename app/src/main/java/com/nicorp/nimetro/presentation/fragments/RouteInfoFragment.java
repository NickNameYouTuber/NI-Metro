package com.nicorp.nimetro.presentation.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class RouteInfoFragment extends Fragment {

    private static final String ARG_ROUTE = "route";
    private static final int COLLAPSED_HEIGHT = 228;

    private List<Station> route;
    private BottomSheetBehavior<FrameLayout> behavior;

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

    public static RouteInfoFragment newInstance(List<Station> route) {
        RouteInfoFragment fragment = new RouteInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROUTE, new Route(route));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            route = ((Route) getArguments().getSerializable(ARG_ROUTE)).getStations();
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

        // Initialize BottomSheetBehavior
        FrameLayout bottomSheet = view.findViewById(R.id.bottomSheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight((int) (COLLAPSED_HEIGHT * getResources().getDisplayMetrics().density));
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private boolean isExpanding = false;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        isExpanding = true;
                        routeTitle.setText("Информация о маршруте");
                        layoutCollapsed.setVisibility(View.GONE);
                        layoutExpanded.setVisibility(View.VISIBLE);

                        // Post the visibility change with a delay to ensure proper layout
                        bottomSheet.post(new Runnable() {
                            @Override
                            public void run() {
                                routeDetailsContainer.setVisibility(View.VISIBLE);
                                // Force a layout pass
                                bottomSheet.requestLayout();
                            }
                        });
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        isExpanding = false;
                        routeTitle.setText("Краткая информация");
                        layoutCollapsed.setVisibility(View.VISIBLE);
                        layoutExpanded.setVisibility(View.GONE);
                        routeDetailsContainer.setVisibility(View.GONE);
                        break;

                    case BottomSheetBehavior.STATE_DRAGGING:
                        // If we're dragging from collapsed state, make sure expanded layout is ready
                        if (!isExpanding && behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                            layoutExpanded.setVisibility(View.VISIBLE);
                            layoutExpanded.setAlpha(0);
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0) {
                    // Update alpha values for smooth transition
                    layoutCollapsed.setAlpha(1 - slideOffset);
                    layoutExpanded.setAlpha(slideOffset);

                    // Show route details gradually as we pass halfway point
                    if (slideOffset > 0.5) {
                        if (routeDetailsContainer.getVisibility() != View.VISIBLE) {
                            routeDetailsContainer.setVisibility(View.VISIBLE);
                            routeDetailsContainer.setAlpha(0);
                        }
                        routeDetailsContainer.setAlpha((slideOffset - 0.5f) * 2);
                    } else {
                        routeDetailsContainer.setVisibility(View.GONE);
                    }
                }
            }
        });

        // Set initial state after a short delay to ensure proper layout
        view.post(new Runnable() {
            @Override
            public void run() {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        return view;
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
                    params.setMargins(0, 8, 0, 8); // Add some vertical spacing
                    stationView.setLayoutParams(params);

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