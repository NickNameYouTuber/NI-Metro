package com.nicorp.nimetro.presentation.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private Context context;
    private List<Station> stations;
    private Set<Integer> selectedPositions;

    public StationAdapter(Context context, List<Station> stations) {
        this.context = context;
        this.stations = stations;
        this.selectedPositions = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.station_adapter_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Station station = stations.get(position);
        holder.textViewStationName.setText(station.getName());
        holder.viewLineColor.setBackgroundColor(Color.parseColor(station.getColor()));

        // Update checkbox state without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedPositions.contains(position));

        // Set up click listeners
        View.OnClickListener clickListener = v -> toggleSelection(position, holder.checkBox);

        holder.itemView.setOnClickListener(clickListener);
        holder.checkBox.setOnClickListener(clickListener);
    }

    private void toggleSelection(int position, CheckBox checkBox) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
            checkBox.setChecked(false);
        } else {
            selectedPositions.add(position);
            checkBox.setChecked(true);
        }
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public List<Station> getSelectedStations() {
        List<Station> selectedStations = new ArrayList<>();
        for (Integer position : selectedPositions) {
            if (position < stations.size()) {
                selectedStations.add(stations.get(position));
                Log.d("StationAdapter", "Selected station: " + stations.get(position).getName());
            }
        }
        return selectedStations;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewStationName;
        View viewLineColor;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewStationName = itemView.findViewById(R.id.textViewStationName);
            viewLineColor = itemView.findViewById(R.id.viewLineColor);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}