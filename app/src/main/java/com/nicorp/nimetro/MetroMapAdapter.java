package com.nicorp.nimetro;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;


public class MetroMapAdapter extends ArrayAdapter<MetroMapItem> {

    private Context context;
    private List<MetroMapItem> metroMapItems;

    public MetroMapAdapter(Context context, List<MetroMapItem> metroMapItems) {
        super(context, 0, metroMapItems);
        this.context = context;
        this.metroMapItems = metroMapItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_metro_map, parent, false);
        }

        ImageView iconImageView = convertView.findViewById(R.id.iconImageView);
        TextView nameTextView = convertView.findViewById(R.id.nameTextView);
        TextView countryTextView = convertView.findViewById(R.id.countryTextView);

        MetroMapItem currentItem = metroMapItems.get(position);

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
}