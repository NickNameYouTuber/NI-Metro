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

/**
 * A bottom sheet fragment that displays detailed information about a metro route.
 * This includes the route's total time, number of stations, number of transfers,
 * and a detailed list of stations with transfer indicators.
 */
public class BottomFragment extends BottomSheetDialogFragment {

    private static final int COLLAPSED_HEIGHT = 228;
    private List<Station> route;
    private BottomSheetBehavior<View> behavior;

    /**
     * Creates a new instance of BottomFragment with the necessary arguments.
     *
     * @param route The route to display information about.
     * @return A new instance of BottomFragment.
     */
    public static BottomFragment newInstance(List<Station> route) {
        BottomFragment fragment = new BottomFragment();
        Bundle args = new Bundle();
        args.putParcelable("route", new Route(route));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Route route1 = requireArguments().getParcelable("route");
            route = route1.getStations();
        }
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        int colorOnSurface = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);

        initializeViews(view, colorOnSurface);
        calculateAndSetRouteStatistics();
        setupBottomSheetBehavior(view);

        return view;
    }

    /**
     * Initializes the views used in the fragment.
     *
     * @param view The root view of the fragment.
     * @param colorOnSurface The color to use for text views.
     */
    private void initializeViews(View view, int colorOnSurface) {
        TextView routeTime = view.findViewById(R.id.routeTime);
        TextView routeStationsCount = view.findViewById(R.id.routeStationsCount);
        TextView routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        TextView routeTitle = view.findViewById(R.id.routeTitle);
        TextView routeTimeTitle = view.findViewById(R.id.routeTimeTitle);
        TextView routeStationsCountTitle = view.findViewById(R.id.routeStationsTitle);
        TextView routeTransfersCountTitle = view.findViewById(R.id.routeTransfersTitle);
        LinearLayout routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);
        View bottomSheetInternal = view.findViewById(R.id.bottom_sheet_internal);

        routeTime.setTextColor(colorOnSurface);
        routeStationsCount.setTextColor(colorOnSurface);
        routeTransfersCount.setTextColor(colorOnSurface);
        routeTitle.setTextColor(colorOnSurface);
        routeTimeTitle.setTextColor(colorOnSurface);
        routeStationsCountTitle.setTextColor(colorOnSurface);
        routeTransfersCountTitle.setTextColor(colorOnSurface);

        behavior = BottomSheetBehavior.from(bottomSheetInternal);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * Calculates and sets the route statistics (total time, number of stations, number of transfers).
     */
    private void calculateAndSetRouteStatistics() {
        if (route != null && !route.isEmpty()) {
            int totalTime = calculateTotalTime();
            int stationsCount = route.size();
            int transfersCount = calculateTransfersCount();

            TextView routeTime = requireView().findViewById(R.id.routeTime);
            TextView routeStationsCount = requireView().findViewById(R.id.routeStationsCount);
            TextView routeTransfersCount = requireView().findViewById(R.id.routeTransfersCount);

            routeTime.setText(String.format("%d мин", totalTime));
            routeStationsCount.setText(String.format("%d", stationsCount));
            routeTransfersCount.setText(String.format("%d", transfersCount));

            populateRouteDetails(requireView().findViewById(R.id.routeDetailsContainer));
        }
    }

    /**
     * Sets up the BottomSheetBehavior to handle state changes and sliding.
     *
     * @param view The root view of the fragment.
     */
    private void setupBottomSheetBehavior(View view) {
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                TextView routeTitle = view.findViewById(R.id.routeTitle);
                LinearLayout routeDetailsContainer = view.findViewById(R.id.routeDetailsContainer);

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

        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        return dialog;
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        String currentColor = null;

        for (int i = 0; i < route.size(); i++) {
            Station station = route.get(i);

            View stationView = inflater.inflate(R.layout.item_route_station, container, false);
            TextView stationName = stationView.findViewById(R.id.stationName);
            View stationIndicator = stationView.findViewById(R.id.stationIndicator);

            stationName.setText(station.getName());
            stationIndicator.setBackgroundColor(Color.parseColor(station.getColor()));

            container.addView(stationView);

            if (i < route.size() - 1 && !route.get(i + 1).getColor().equals(station.getColor())) {
                View transferView = inflater.inflate(R.layout.item_transfer_indicator, container, false);
                container.addView(transferView);
            }
        }
    }
}