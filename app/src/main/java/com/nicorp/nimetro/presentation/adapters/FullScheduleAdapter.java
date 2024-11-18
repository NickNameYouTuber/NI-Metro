package com.nicorp.nimetro.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nicorp.nimetro.R;
import com.nicorp.nimetro.data.models.YandexRaspResponse;

import java.util.List;

public class FullScheduleAdapter extends RecyclerView.Adapter<FullScheduleAdapter.FullScheduleViewHolder> {

    private List<YandexRaspResponse.Segment> segments;

    public FullScheduleAdapter(List<YandexRaspResponse.Segment> segments) {
        this.segments = segments;
    }

    @NonNull
    @Override
    public FullScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_full_schedule, parent, false);
        return new FullScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FullScheduleViewHolder holder, int position) {
        holder.bind(segments.get(position));
    }

    @Override
    public int getItemCount() {
        return segments.size();
    }

    public static class FullScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView trainNumber;
        private TextView departureTime;
        private TextView arrivalTime;
        private TextView duration;
        private TextView trainTitle;
        private TextView carrierTitle;

        public FullScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            trainNumber = itemView.findViewById(R.id.trainNumber);
            departureTime = itemView.findViewById(R.id.departureTime);
            arrivalTime = itemView.findViewById(R.id.arrivalTime);
            duration = itemView.findViewById(R.id.duration);
            trainTitle = itemView.findViewById(R.id.trainTitle);
            carrierTitle = itemView.findViewById(R.id.carrierTitle);
        }

        public void bind(YandexRaspResponse.Segment segment) {
            trainNumber.setText(segment.getThread().getNumber());
            departureTime.setText(segment.getDeparture().substring(11, 16));
            arrivalTime.setText(segment.getArrival().substring(11, 16));
            duration.setText(String.valueOf((int) (segment.getDuration() / 60)) + " мин");
            trainTitle.setText(segment.getThread().getTitle());
            carrierTitle.setText(segment.getThread().getCarrier().getTitle());
        }
    }
}