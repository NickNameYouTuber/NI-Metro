package com.nicorp.nimetro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.List;

public class MetroMapExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<MetroMapGroup> metroMapGroups;

    public MetroMapExpandableListAdapter(Context context, List<MetroMapGroup> metroMapGroups) {
        this.context = context;
        this.metroMapGroups = metroMapGroups;
    }

    @Override
    public int getGroupCount() {
        return metroMapGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return metroMapGroups.get(groupPosition).getMetroMapItems().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return metroMapGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return metroMapGroups.get(groupPosition).getMetroMapItems().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_metro_map_group, parent, false);
        }

        TextView countryTextView = convertView.findViewById(R.id.countryTextView);
        countryTextView.setText(metroMapGroups.get(groupPosition).getCountry());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_metro_map, parent, false);
        }

        ImageView iconImageView = convertView.findViewById(R.id.iconImageView);
        TextView nameTextView = convertView.findViewById(R.id.nameTextView);

        MetroMapItem currentItem = metroMapGroups.get(groupPosition).getMetroMapItems().get(childPosition);

        if (currentItem != null) {
            Glide.with(context).load(currentItem.getIconUrl()).into(iconImageView);
            nameTextView.setText(currentItem.getName());
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}