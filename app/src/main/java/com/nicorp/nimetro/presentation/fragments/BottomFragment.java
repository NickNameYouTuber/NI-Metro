package com.nicorp.nimetro.presentation.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class BottomFragment extends BottomSheetDialogFragment {

    private static final int COLLAPSED_HEIGHT = 228;
    private List<Station> route;
    private BottomSheetBehavior<View> behavior;

    public static BottomFragment newInstance(List<Station> route) {
        BottomFragment fragment = new BottomFragment();
        Bundle args = new Bundle();
        args.putSerializable("route", new Route(route));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            route = ((Route) getArguments().getSerializable("route")).getStations();
        }
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        int colorOnSurface = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

        // Initialize views
        TextView routeTime = view.findViewById(R.id.routeTime);
        TextView routeStationsCount = view.findViewById(R.id.routeStationsCount);
        TextView routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        TextView routeTitle = view.findViewById(R.id.routeTitle);
        TextView routeTimeTitle = view.findViewById(R.id.routeTimeTitle);
        TextView routeStationsCountTitle = view.findViewById(R.id.routeStationsTitle);
        TextView routeTransfersCountTitle = view.findViewById(R.id.routeTransfersTitle);
        LinearLayout routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        View bottomSheetInternal = view.findViewById(R.id.bottom_sheet_internal);

        // Set text view colors to colorOnSurface
        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);

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

    @Override
    public void onStart() {
        super.onStart();

        float density = requireContext().getResources().getDisplayMetrics().density;

        if (getDialog() != null) {
            FrameLayout bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

            behavior.setPeekHeight((int) (COLLAPSED_HEIGHT * density));
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Allow outside touch to dismiss the dialog
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Set the background to be transparent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        return dialog;
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