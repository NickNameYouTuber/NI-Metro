package com.nicorp.nimetro;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteInfoDialogFragment extends DialogFragment {

    private static final String ARG_ROUTE = "route";

    private List<Station> route;

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
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Route Info");
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_route_info, container, false);

        TextView routeTime = view.findViewById(R.id.routeTime);
        TextView routeStationsCount = view.findViewById(R.id.routeStationsCount);
        TextView routeTransfersCount = view.findViewById(R.id.routeTransfersCount);
        TextView departureStation = view.findViewById(R.id.departureStation);
        LinearLayout intermediateStationsContainer = view.findViewById(R.id.intermediateStationsContainer);
        TextView arrivalStation = view.findViewById(R.id.arrivalStation);

        if (route != null && route.size() > 0) {
            // Calculate route time, stations count, and transfers count
            int totalTime = route.size() * 2; // Assuming each station takes 2 minutes
            int stationsCount = route.size();
            int transfersCount = 0;
            for (int i = 1; i < route.size(); i++) {
                if (route.get(i).getColor() != route.get(i - 1).getColor()) {
                    transfersCount++;
                }
            }

            routeTime.setText("Время: " + totalTime + " мин");
            routeStationsCount.setText("Количество станций: " + stationsCount);
            routeTransfersCount.setText("Количество пересадок: " + transfersCount);

            // Display departure station
            departureStation.setText("Станция отправления: " + route.get(0).getName());

            // Prepare data for ExpandableListView
            List<String> groupNames = new ArrayList<>();
            Map<String, List<String>> childNames = new HashMap<>();

            String currentGroupName = "";
            List<String> currentChildNames = new ArrayList<>();

            for (int i = 1; i < route.size() - 1; i++) {
                if (route.get(i).getColor() != route.get(i - 1).getColor()) {
                    if (!currentGroupName.isEmpty()) {
                        groupNames.add(currentGroupName);
                        childNames.put(currentGroupName, currentChildNames);
                    }
                    currentGroupName = currentChildNames.size() + " станций";
                    currentChildNames = new ArrayList<>();
                }
                currentChildNames.add(route.get(i).getName());
            }

            if (!currentGroupName.isEmpty()) {
                groupNames.add(currentGroupName);
                childNames.put(currentGroupName, currentChildNames);
            }

            // Dynamically create ExpandableListView and transferInfo for each group
            for (int i = 0; i < groupNames.size(); i++) {
                String groupName = groupNames.get(i);
                List<String> childList = childNames.get(groupName);

                // Create ExpandableListView
                ExpandableListView expandableListView = new ExpandableListView(getContext());
                RouteExpandableListAdapter adapter = new RouteExpandableListAdapter(getContext(), new ArrayList<String>() {{ add(groupName); }}, new HashMap<String, List<String>>() {{ put(groupName, childList); }});
                expandableListView.setAdapter(adapter);
                intermediateStationsContainer.addView(expandableListView);

                // Create transferInfo if not the last group
                if (i < groupNames.size() - 1) {
                    TextView transferInfo = new TextView(getContext());
                    transferInfo.setText("Переход на другую линию");
                    transferInfo.setTextColor(getResources().getColor(R.color.text_primary));
                    transferInfo.setTextSize(15);
                    transferInfo.setPadding(0, 12, 0, 12);
                    intermediateStationsContainer.addView(transferInfo);
                }
            }

            // Display arrival station
            arrivalStation.setText("Станция прибытия: " + route.get(route.size() - 1).getName());
        }

        return view;
    }
}