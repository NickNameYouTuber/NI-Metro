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

/**
 * Adapter for displaying a list of stations in a spinner.
 */
public class StationSpinnerAdapter extends ArrayAdapter<Station> {

    private List<Station> stations;

    /**
     * Creates a new instance of StationSpinnerAdapter with the necessary arguments.
     *
     * @param context The context of the spinner.
     * @param stations The list of stations to display.
     */
    public StationSpinnerAdapter(Context context, List<Station> stations) {
        super(context, 0, stations);
        this.stations = stations;
    }

    /**
     * Creates a new instance of StationSpinnerAdapter with the necessary arguments.
     * @param position The position of the item in the spinner.
     * @param convertView The view to reuse if possible.
     * @param parent The parent view.
     **/
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    /**
     * Creates a new instance of StationSpinnerAdapter with the necessary arguments.
     * @param position The position of the item in the spinner.
     * @param convertView The view to reuse if possible.
     * @param parent The parent view.
     **/
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    /**
     * Creates a new instance of StationSpinnerAdapter with the necessary arguments.
     * @param position The position of the item in the spinner.
     * @param convertView The view to reuse if possible.
     * @param parent The parent view.
     **/
    private View initView(int position, View convertView, ViewGroup parent) {
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

    /**
     * Filters the list of stations based on the query.
     *
     * @param query The query to filter by.
     */
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