package com.nicorp.nimetro.presentation.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class StationListAdapter extends ArrayAdapter<Station> {

    private List<Station> stations;

    public StationListAdapter(Context context, List<Station> stations) {
        super(context, 0, stations);
        this.stations = stations;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_station_list, parent, false);
        }

        Station station = getItem(position);

        View stationColorView = convertView.findViewById(R.id.stationColorView);
        TextView stationNameTextView = convertView.findViewById(R.id.stationNameTextView);
        TextView stationIdTextView = convertView.findViewById(R.id.stationIdTextView);

        stationColorView.setBackgroundColor(Color.parseColor(station.getColor()));
        stationNameTextView.setText(station.getName());
        stationIdTextView.setText(String.valueOf(station.getId()));

        return convertView;
    }

    public void filter(String query) {
        query = query.toLowerCase();
        clear();
        if (query.isEmpty()) {
            addAll(stations);
        } else {
            for (Station station : stations) {
                if (station.getName().toLowerCase().contains(query)) {
                    add(station);
                }
            }
        }
        notifyDataSetChanged();
    }
}