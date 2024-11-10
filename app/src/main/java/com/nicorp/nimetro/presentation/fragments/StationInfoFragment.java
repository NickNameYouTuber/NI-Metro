package com.nicorp.nimetro.presentation.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Line;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.domain.entities.Transfer;

import java.io.Serializable;
import java.util.List;

/**
 * A fragment that displays detailed information about a specific metro station.
 * This includes the station's name, previous and next stations, line information,
 * and transfer details.
 */
public class StationInfoFragment extends Fragment {

    private static final String ARG_LINE = "line";
    private static final String ARG_STATION = "station";
    private static final String ARG_PREV_STATION = "prev_station";
    private static final String ARG_NEXT_STATION = "next_station";
    private static final String ARG_TRANSFER = "transfers";
    private static final String ARG_LINES = "lines";

    private Line line;
    private List<Line> lines;
    private Station station;
    private List<Transfer> transfers;
    private Station prevStation;
    private Station nextStation;
    private OnStationInfoListener listener;

    /**
     * Creates a new instance of StationInfoFragment with the necessary arguments.
     *
     * @param line The current line of the station.
     * @param station The station to display information about.
     * @param prevStation The previous station on the line.
     * @param nextStation The next station on the line.
     * @param transfers List of transfers available from the station.
     * @param lines List of all lines in the metro system.
     * @return A new instance of StationInfoFragment.
     */
    public static StationInfoFragment newInstance(Line line, Station station, Station prevStation, Station nextStation, List<Transfer> transfers, List<Line> lines) {
        StationInfoFragment fragment = new StationInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_STATION, station);
        args.putParcelable(ARG_PREV_STATION, prevStation);
        args.putParcelable(ARG_NEXT_STATION, nextStation);
        args.putParcelable(ARG_LINE, line);
        args.putSerializable(ARG_TRANSFER, (Serializable) transfers);
        args.putSerializable(ARG_LINES, (Serializable) lines);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            station = getArguments().getParcelable(ARG_STATION);
            prevStation = getArguments().getParcelable(ARG_PREV_STATION);
            nextStation = getArguments().getParcelable(ARG_NEXT_STATION);
            line = getArguments().getParcelable(ARG_LINE);
            transfers = (List<Transfer>) getArguments().getSerializable(ARG_TRANSFER);
            lines = (List<Line>) getArguments().getSerializable(ARG_LINES);
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
        setStationNameVisibility(prevStationName, prevStation);

        TextView nextStationName = view.findViewById(R.id.nextStationName);
        setStationNameVisibility(nextStationName, nextStation);

        TextView fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> onFromButtonClick());

        TextView toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> onToButtonClick());

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        TextView lineNumber = view.findViewById(R.id.lineNumber);
        setLineNumberAndColor(lineNumber, station);

        LinearLayout transferCirclesContainer = view.findViewById(R.id.transferCirclesContainer);
        addTransferCircles(transferCirclesContainer);

        return view;
    }

    /**
     * Sets the visibility of the station name TextView based on whether the station is null.
     *
     * @param stationNameTextView The TextView to set visibility for.
     * @param station The station to check for null.
     */
    private void setStationNameVisibility(TextView stationNameTextView, Station station) {
        if (station != null) {
            stationNameTextView.setText(station.getName());
            stationNameTextView.setVisibility(View.VISIBLE);
        } else {
            stationNameTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Handles the click event for the "From" button.
     * Notifies the listener to set the current station as the start station.
     */
    private void onFromButtonClick() {
        if (listener != null) {
            listener.onSetStart(station, true);
        }
        dismiss();
    }

    /**
     * Handles the click event for the "To" button.
     * Notifies the listener to set the current station as the end station.
     */
    private void onToButtonClick() {
        if (listener != null) {
            listener.onSetEnd(station, true);
        }
        dismiss();
    }

    /**
     * Sets the line number and color for the station.
     *
     * @param lineNumber The TextView to display the line number.
     * @param station The station to get the line information from.
     */
    private void setLineNumberAndColor(TextView lineNumber, Station station) {
        lineNumber.setText(String.valueOf(line.getLineIdForStation(station)));
        lineNumber.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(station.getColor())));
    }

    /**
     * Adds transfer circles to the container for each transfer available from the station.
     *
     * @param transferCirclesContainer The container to add transfer circles to.
     */
    private void addTransferCircles(LinearLayout transferCirclesContainer) {
        for (Transfer transfer : transfers) {
            if (transfer.getStations().contains(station)) {
                for (Station transferStation : transfer.getStations()) {
                    if (!transferStation.equals(station)) {
                        Log.d("StationInfoFragment", "Transfer station: " + transferStation);

                        TextView transferCircle = createTransferCircle(transferStation);
                        transferCirclesContainer.addView(transferCircle);
                    }
                }
            }
        }
    }

    /**
     * Creates a transfer circle TextView for the given transfer station.
     *
     * @param transferStation The transfer station to create the circle for.
     * @return The created TextView representing the transfer circle.
     */
    private TextView createTransferCircle(Station transferStation) {
        TextView transferCircle = new TextView(getContext());
        int transferLineId = getLineIdForStation(transferStation);
        transferCircle.setText(String.valueOf(transferLineId));
        transferCircle.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.circle_background_red));
        transferCircle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(getColorForStation(transferStation))));
        transferCircle.setGravity(Gravity.CENTER);
        transferCircle.setTextColor(Color.WHITE);
        transferCircle.setTextSize(12);
        transferCircle.setPadding(4, 4, 4, 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        transferCircle.setLayoutParams(params);
        return transferCircle;
    }

    /**
     * Gets the line ID for the given station.
     *
     * @param station The station to get the line ID for.
     * @return The line ID of the station, or -1 if not found.
     */
    private int getLineIdForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line.getId();
            }
        }
        return -1;
    }

    /**
     * Gets the color for the given station.
     *
     * @param station The station to get the color for.
     * @return The color of the station, or black if not found.
     */
    private String getColorForStation(Station station) {
        for (Line line : lines) {
            if (line.getStations().contains(station)) {
                return line.getColor();
            }
        }
        return "#000000";
    }

    /**
     * Sets the listener for station info events.
     *
     * @param listener The listener to set.
     */
    public void setOnStationInfoListener(OnStationInfoListener listener) {
        this.listener = listener;
    }

    /**
     * Dismisses the fragment.
     */
    private void dismiss() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
        }
    }

    /**
     * Interface for handling station info events.
     */
    public interface OnStationInfoListener {
        void onSetStart(Station station, boolean fromStationInfoFragment);
        void onSetEnd(Station station, boolean fromStationInfoFragment);
    }
}