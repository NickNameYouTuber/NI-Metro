package com.nicorp.nimetro.presentation.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class StationsAdapter extends RecyclerView.Adapter<StationsAdapter.StationViewHolder> {

    private List<Station> stations;
    private OnStationClickListener listener;

    public StationsAdapter(List<Station> stations, OnStationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        Station station = stations.get(position);
        holder.bind(station);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public class StationViewHolder extends RecyclerView.ViewHolder {

        private TextView stationNameTextView;
        private View lineColorView;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationNameTextView = itemView.findViewById(R.id.stationNameTextView);
            lineColorView = itemView.findViewById(R.id.lineColorView);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStationSelected(stations.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Station station) {
            stationNameTextView.setText(station.getName());
            lineColorView.setBackgroundColor(Color.parseColor(station.getColor()));
        }
    }

    public interface OnStationClickListener {
        void onStationSelected(Station station);
    }
}