package com.nicorp.nimetro;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.List;

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

        TextView routeInfo = view.findViewById(R.id.routeInfo);
        StringBuilder routeText = new StringBuilder();
        for (Station station : route) {
            routeText.append(station.getName()).append("\n");
        }
        routeInfo.setText(routeText.toString());

        return view;
    }
}