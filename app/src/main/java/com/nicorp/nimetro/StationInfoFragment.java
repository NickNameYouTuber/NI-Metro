package com.nicorp.nimetro;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StationInfoFragment extends Fragment {

    private static final String ARG_LINE = "line";
    private static final String ARG_STATION = "station";
    private static final String ARG_PREV_STATION = "prev_station";
    private static final String ARG_NEXT_STATION = "next_station";

    private Line line;
    private Station station;
    private Station prevStation;
    private Station nextStation;
    private OnStationInfoListener listener;

    public static StationInfoFragment newInstance(Line line, Station station, Station prevStation, Station nextStation) {
        StationInfoFragment fragment = new StationInfoFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATION, station);
        args.putSerializable(ARG_PREV_STATION, prevStation);
        args.putSerializable(ARG_NEXT_STATION, nextStation);
        args.putSerializable(ARG_LINE, line);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            station = (Station) getArguments().getSerializable(ARG_STATION);
            prevStation = (Station) getArguments().getSerializable(ARG_PREV_STATION);
            nextStation = (Station) getArguments().getSerializable(ARG_NEXT_STATION);
            line = (Line) getArguments().getSerializable(ARG_LINE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_station_info, container, false);

        // Set view width to match parent
        view.setMinimumWidth(LinearLayout.LayoutParams.MATCH_PARENT);

        TextView stationName = view.findViewById(R.id.stationName);
        stationName.setText(station.getName());

        TextView prevStationName = view.findViewById(R.id.prevStationName);
        if (prevStation != null) {
            prevStationName.setText("Предыдущая: " + prevStation.getName());
            prevStationName.setVisibility(View.VISIBLE);
        } else {
            prevStationName.setVisibility(View.GONE);
        }

        TextView nextStationName = view.findViewById(R.id.nextStationName);
        if (nextStation != null) {
            nextStationName.setText("Следующая: " + nextStation.getName());
            nextStationName.setVisibility(View.VISIBLE);
        } else {
            nextStationName.setVisibility(View.GONE);
        }

        TextView fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetStart(station);
            }
            dismiss();
        });

        TextView toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetEnd(station);
            }
            dismiss();
        });

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        // Установите цвет линии и номер линии
        TextView lineNumber = view.findViewById(R.id.lineNumber);
        lineNumber.setText(String.valueOf(line.getLineIdForStation(station)));
        lineNumber.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(station.getColor())));

        return view;
    }

    public void setOnStationInfoListener(OnStationInfoListener listener) {
        this.listener = listener;
    }

    private void dismiss() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
        }
    }

    public interface OnStationInfoListener {
        void onSetStart(Station station);
        void onSetEnd(Station station);
    }
}