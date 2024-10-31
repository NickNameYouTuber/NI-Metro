package com.nicorp.nimetro;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MetroAdapter extends RecyclerView.Adapter<MetroAdapter.MetroViewHolder> {

    private List<Station> stations;
    private List<Line> lines;
    private OnStationClickListener listener;
    private List<Station> route;

    public MetroAdapter(List<Station> stations, List<Line> lines, OnStationClickListener listener) {
        this.stations = stations;
        this.lines = lines;
        this.listener = listener;
    }

    public void setRoute(List<Station> route) {
        this.route = route;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MetroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new MetroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetroViewHolder holder, int position) {
        Station station = stations.get(position);
        holder.stationName.setText(station.getName());
        holder.stationName.setTextColor(Color.parseColor(station.getColor()));
        holder.itemView.setOnClickListener(v -> listener.onStationClick(station));

        if (route != null && route.contains(station)) {
            holder.itemView.setBackgroundColor(Color.YELLOW);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    static class MetroViewHolder extends RecyclerView.ViewHolder {
        TextView stationName;

        public MetroViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.stationName);
        }
    }
}