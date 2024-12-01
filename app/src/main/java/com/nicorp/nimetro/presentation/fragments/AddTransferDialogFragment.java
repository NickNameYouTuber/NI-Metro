package com.nicorp.nimetro.presentation.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;
import com.nicorp.nimetro.presentation.adapters.StationAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AddTransferDialogFragment extends DialogFragment {

    private List<Station> stations;
    private OnTransferAddedListener listener;
    private StationAdapter adapter;

    public interface OnTransferAddedListener {
        void onTransferAdded(List<Station> selectedStations);
    }

    public AddTransferDialogFragment(List<Station> stations, OnTransferAddedListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_transfer, null);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewStations);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new StationAdapter(getActivity(), stations);
        recyclerView.setAdapter(adapter);

        Button buttonAdd = view.findViewById(R.id.buttonAdd);
        buttonAdd.setOnClickListener(v -> {
            List<Station> selectedStations = adapter.getSelectedStations();
            for (Station station : selectedStations) {
                Log.d("AddTransferDialogFragment", station.getName());
            }
            listener.onTransferAdded(selectedStations);
            dismiss();
        });

        builder.setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddTransferDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}