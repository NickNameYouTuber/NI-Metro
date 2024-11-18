package com.nicorp.nimetro.presentation.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.models.YandexRaspResponse;

import java.util.List;

public class TrainInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TRAIN_INFO = 0;
    private static final int TYPE_MORE_TRAINS = 1;

    private List<YandexRaspResponse.Segment> segments;
    private OnMoreTrainsClickListener onMoreTrainsClickListener;

    public TrainInfoAdapter(List<YandexRaspResponse.Segment> segments, OnMoreTrainsClickListener onMoreTrainsClickListener) {
        this.segments = segments;
        this.onMoreTrainsClickListener = onMoreTrainsClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_TRAIN_INFO) {
            View view = inflater.inflate(R.layout.item_train_info, parent, false);
            return new TrainInfoViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_more_trains, parent, false);
            return new MoreTrainsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TrainInfoViewHolder) {
            ((TrainInfoViewHolder) holder).bind(segments.get(position));
        } else if (holder instanceof MoreTrainsViewHolder) {
            ((MoreTrainsViewHolder) holder).bind(onMoreTrainsClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return segments.size() + 1; // +1 для элемента "Еще"
    }

    @Override
    public int getItemViewType(int position) {
        if (position < segments.size()) {
            return TYPE_TRAIN_INFO;
        } else {
            return TYPE_MORE_TRAINS;
        }
    }

    public static class TrainInfoViewHolder extends RecyclerView.ViewHolder {
        private TextView trainNumber;
        private TextView departureTime;
        private TextView arrivalTime;
        private TextView duration;

        public TrainInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            trainNumber = itemView.findViewById(R.id.trainNumber);
            departureTime = itemView.findViewById(R.id.departureTime);
            arrivalTime = itemView.findViewById(R.id.arrivalTime);
            duration = itemView.findViewById(R.id.duration);
        }

        @SuppressLint("SetTextI18n")
        public void bind(YandexRaspResponse.Segment segment) {
            trainNumber.setText(segment.getThread().getNumber());
            departureTime.setText(segment.getDeparture().substring(11, 16));
            arrivalTime.setText(segment.getArrival().substring(11, 16));
            duration.setText((String.valueOf((int) (segment.getDuration() / 60))) + " мин");
        }
    }

    public static class MoreTrainsViewHolder extends RecyclerView.ViewHolder {
        private TextView moreTrainsText;

        public MoreTrainsViewHolder(@NonNull View itemView) {
            super(itemView);
            moreTrainsText = itemView.findViewById(R.id.moreTrainsText);
        }

        public void bind(OnMoreTrainsClickListener listener) {
            itemView.setOnClickListener(v -> listener.onMoreTrainsClick());
        }
    }

    public interface OnMoreTrainsClickListener {
        void onMoreTrainsClick();
    }
}