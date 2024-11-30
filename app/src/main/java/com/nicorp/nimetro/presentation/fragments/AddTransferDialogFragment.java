package com.nicorp.nimetro.presentation.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.nicorp.nimetro.domain.entities.Station;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

public class AddTransferDialogFragment extends DialogFragment {

    private List<Station> stations;
    private OnTransferAddedListener listener;
    private boolean[] checkedItems;

    public interface OnTransferAddedListener {
        void onTransferAdded(List<Station> selectedStations);
    }

    public AddTransferDialogFragment(List<Station> stations, OnTransferAddedListener listener) {
        this.stations = stations;
        this.listener = listener;
        this.checkedItems = new boolean[stations.size()];
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Выберите станции для перехода")
                .setMultiChoiceItems(getStationNames(), checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                })
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        List<Station> selectedStations = new ArrayList<>();
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                selectedStations.add(stations.get(i));
                            }
                        }
                        listener.onTransferAdded(selectedStations);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddTransferDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    private String[] getStationNames() {
        String[] names = new String[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            names[i] = stations.get(i).getName();
        }
        return names;
    }
}