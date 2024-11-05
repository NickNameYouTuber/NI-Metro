package com.nicorp.nimetro;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

public class StationInfoDialogFragment extends DialogFragment {

    private static final String ARG_STATION = "station";

    private Station station;
    private OnStationInfoListener listener;

    public static StationInfoDialogFragment newInstance(Station station) {
        StationInfoDialogFragment fragment = new StationInfoDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATION, station);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.TransparentDialog);
        if (getArguments() != null) {
            station = (Station) getArguments().getSerializable(ARG_STATION);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(station.getName());

        // Устанавливаем прозрачный фон
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_station_info, container, false);

        // Set view width to match parent
        view.setMinimumWidth(LinearLayout.LayoutParams.MATCH_PARENT);

        TextView stationName = view.findViewById(R.id.stationName);
        stationName.setText(station.getName());

        TextView scheduleText = view.findViewById(R.id.scheduleText);
        scheduleText.setText("Расписание: " + station.getFacilities().getSchedule());

        TextView escalatorsText = view.findViewById(R.id.escalatorsText);
        escalatorsText.setText("Эскалаторов: " + station.getFacilities().getEscalators());

        TextView elevatorsText = view.findViewById(R.id.elevatorsText);
        elevatorsText.setText("Лифтов: " + station.getFacilities().getElevators());

        TextView exitsText = view.findViewById(R.id.exitsText);
        StringBuilder exitsBuilder = new StringBuilder();
        for (String exit : station.getFacilities().getExits()) {
            exitsBuilder.append(exit).append("\n");
        }
        exitsText.setText("Выходы: " + exitsBuilder.toString());

        Button fromButton = view.findViewById(R.id.fromButton);
        fromButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetStart(station);
            }
            dismiss();
        });

        Button toButton = view.findViewById(R.id.toButton);
        toButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetEnd(station);
            }
            dismiss();
        });

        MaterialButton closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    public void setOnStationInfoListener(OnStationInfoListener listener) {
        this.listener = listener;
    }

    public interface OnStationInfoListener {
        void onSetStart(Station station);
        void onSetEnd(Station station);
    }
}