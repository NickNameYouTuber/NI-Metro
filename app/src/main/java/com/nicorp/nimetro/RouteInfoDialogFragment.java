package com.nicorp.nimetro;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;

import java.util.List;

public class RouteInfoDialogFragment extends BottomSheetDialogFragment {

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

    public static RouteInfoDialogFragment newInstance(List<Station> route) {
        RouteInfoDialogFragment fragment = new RouteInfoDialogFragment();
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
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
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

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        float density = requireContext().getResources().getDisplayMetrics().density;

        if (getDialog() != null) {
            FrameLayout bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            behavior = BottomSheetBehavior.from(bottomSheet);

            behavior.setPeekHeight((int) (COLLAPSED_HEIGHT * density));
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        routeTitle.setText("Информация о маршруте");
                        routeDetailsContainer.setVisibility(View.VISIBLE);
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        routeTitle.setText("Краткая информация");
                        routeDetailsContainer.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (slideOffset > 0) {
                        layoutCollapsed.setAlpha(1 - 2 * slideOffset);
                        layoutExpanded.setAlpha(slideOffset * slideOffset);

                        if (slideOffset > 0.5) {
                            layoutCollapsed.setVisibility(View.GONE);
                            layoutExpanded.setVisibility(View.VISIBLE);
                        }

                        if (slideOffset < 0.5 && layoutExpanded.getVisibility() == View.VISIBLE) {
                            layoutCollapsed.setVisibility(View.VISIBLE);
                            layoutExpanded.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        }
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String currentColor = null;

        for (int i = 0; i < route.size(); i++) {
            Station station = route.get(i);
            Log.d("RouteInfoDialogFragment", station.getName());

            // Add station
            View stationView = inflater.inflate(R.layout.item_route_station, container, false);
            TextView stationName = stationView.findViewById(R.id.stationName);
            View stationIndicator = stationView.findViewById(R.id.stationIndicator);

            stationName.setText(station.getName());
            stationIndicator.setBackgroundColor(android.graphics.Color.parseColor(station.getColor()));

            container.addView(stationView);

            // Add transfer indicator if needed
            if (i < route.size() - 1 && !route.get(i + 1).getColor().equals(station.getColor())) {
                View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                container.addView(transferView);
            }
        }
    }
}