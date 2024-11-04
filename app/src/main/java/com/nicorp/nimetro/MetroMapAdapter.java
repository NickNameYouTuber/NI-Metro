package com.nicorp.nimetro;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class MetroMapAdapter extends ArrayAdapter<MetroMapItem> {

    private Context context;
    private List<MetroMapItem> metroMapItems;
    private List<MetroMapItem> filteredMetroMapItems;

    public MetroMapAdapter(Context context, List<MetroMapItem> metroMapItems) {
        super(context, 0, metroMapItems);
        this.context = context;
        this.metroMapItems = metroMapItems;
        this.filteredMetroMapItems = new ArrayList<>(metroMapItems);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_metro_map, parent, false);
        }

        ImageView iconImageView = convertView.findViewById(R.id.iconImageView);
        TextView nameTextView = convertView.findViewById(R.id.nameTextView);
        TextView countryTextView = convertView.findViewById(R.id.countryTextView);

        MetroMapItem currentItem = filteredMetroMapItems.get(position);

        if (currentItem != null) {
            // Загрузка изображения с помощью Glide
            Glide.with(context)
                    .load(currentItem.getIconUrl()) // Glide автоматически обрабатывает URL с русскими символами
                    .into(iconImageView);

            nameTextView.setText(currentItem.getName());
            countryTextView.setText(currentItem.getCountry());
            Log.d("MetroMapAdapter", "Country: " + currentItem.getCountry());
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return filteredMetroMapItems.size();
    }

    @Nullable
    @Override
    public MetroMapItem getItem(int position) {
        return filteredMetroMapItems.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<MetroMapItem> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(metroMapItems);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (MetroMapItem item : metroMapItems) {
                        if (item.getName().toLowerCase().contains(filterPattern) || item.getCountry().toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMetroMapItems.clear();
                filteredMetroMapItems.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }
}