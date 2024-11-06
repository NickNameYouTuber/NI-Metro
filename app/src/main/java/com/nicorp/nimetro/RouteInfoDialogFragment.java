package com.nicorp.nimetro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;

public class RouteInfoDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROUTE = "route";
    private List<Station> route;
    private BottomSheetBehavior<View> behavior;

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

        // Initialize views
        TextView routeTime = view.findViewById(R.id.routeTime);
        TextView routeStationsCount = view.findViewById(R.id.routeStationsCount);
        TextView routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        TextView routeTitle = view.findViewById(R.id.routeTitle);
        LinearLayout routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        View bottomSheetInternal = view.findViewById(R.id.bottom_sheet_internal);

        // Setup BottomSheetBehavior
        behavior = BottomSheetBehavior.from(bottomSheetInternal);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

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

        // Handle BottomSheet state changes
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
                // Optional: Implement animations during sliding
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String currentColor = null;

        for (int i = 0; i < route.size(); i++) {
            Station station = route.get(i);

            // Add line header if color changes
//            if (!station.getColor().equals(currentColor)) {
//                View lineHeader = inflater.inflate(R.layout.item_line_header, container, false);
////                View lineIndicator = lineHeader.findViewById(R.id.lineIndicator);
////                lineIndicator.setBackgroundColor(android.graphics.Color.parseColor(station.getColor()));
//                container.addView(lineHeader);
//                currentColor = station.getColor();
//            }

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