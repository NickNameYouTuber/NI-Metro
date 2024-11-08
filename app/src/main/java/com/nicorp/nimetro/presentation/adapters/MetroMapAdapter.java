package com.nicorp.nimetro.presentation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nicorp.nimetro.R;
import com.nicorp.nimetro.domain.entities.MetroMapItem;

import java.util.List;

public class MetroMapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;
    private List<Object> items;
    private OnItemClickListener listener;

    public MetroMapAdapter(Context context, List<Object> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_metro_map_group, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_metro_map, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String country = (String) items.get(position);
            ((HeaderViewHolder) holder).bind(country);
        } else if (holder instanceof ItemViewHolder) {
            MetroMapItem item = (MetroMapItem) items.get(position);
            ((ItemViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, MetroMapItem item);
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView countryTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            countryTextView = itemView.findViewById(R.id.countryTextView);
        }

        public void bind(String country) {
            countryTextView.setText(country);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView iconImageView;
        private TextView nameTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position, (MetroMapItem) items.get(position));
                }
            });
        }

        public void bind(MetroMapItem item) {
            Glide.with(context).load(item.getIconUrl()).into(iconImageView);
            nameTextView.setText(item.getName());
        }
    }
}