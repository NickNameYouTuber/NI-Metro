package com.nicorp.nimetro;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IntermediateStationAdapter extends RecyclerView.Adapter<IntermediateStationAdapter.IntermediateStationViewHolder> {

    private final List<Station> stations;
    private final String lineColor;

    public IntermediateStationAdapter(List<Station> stations, String lineColor) {
        this.stations = stations;
        this.lineColor = lineColor;
    }

    @NonNull
    @Override
    public IntermediateStationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intermediate_station, parent, false);
        return new IntermediateStationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IntermediateStationViewHolder holder, int position) {
        Station station = stations.get(position);
        holder.stationName.setText(station.getName());
        holder.stationDot.setBackgroundColor(Color.parseColor(lineColor));
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public static class IntermediateStationViewHolder extends RecyclerView.ViewHolder {
        TextView stationName;
        View stationDot;

        public IntermediateStationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.stationName);
            stationDot = itemView.findViewById(R.id.stationDot);
        }
    }
}