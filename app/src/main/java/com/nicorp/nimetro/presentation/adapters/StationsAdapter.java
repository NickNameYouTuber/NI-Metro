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
import com.nicorp.nimetro.domain.entities.StationNotification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationsAdapter extends RecyclerView.Adapter<StationsAdapter.StationViewHolder> {

    private List<Station> stations;
    private OnStationClickListener listener;
    private Map<String, StationNotification> stationNotifications;

    public StationsAdapter(List<Station> stations, OnStationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
        this.stationNotifications = new HashMap<>();
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    public void setStationNotifications(Map<String, StationNotification> notifications) {
        this.stationNotifications = notifications != null ? notifications : new HashMap<>();
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
        private TextView stationInfoTextView;
        private View lineColorView;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationNameTextView = itemView.findViewById(R.id.stationNameTextView);
            stationInfoTextView = itemView.findViewById(R.id.stationInfoTextView);
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

            StationNotification notification = stationNotifications.get(station.getId());
            if (notification != null && notification.getText() != null && !notification.getText().isEmpty()) {
                stationInfoTextView.setText(notification.getText());
                stationInfoTextView.setVisibility(View.VISIBLE);

                if (notification.getType() == StationNotification.NotificationType.IMPORTANT) {
                    stationInfoTextView.setBackgroundColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_light, null));
                    stationInfoTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black, null));
                } else {
                    stationInfoTextView.setBackgroundColor(itemView.getContext().getResources().getColor(android.R.color.transparent, null));
                    stationInfoTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray, null));
                }
            } else {
                stationInfoTextView.setVisibility(View.GONE);
            }
        }
    }

    public interface OnStationClickListener {
        void onStationSelected(Station station);
    }
}