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

public class TrainInfoAdapter extends RecyclerView.Adapter<TrainInfoAdapter.TrainInfoViewHolder> {

    private List<YandexRaspResponse.Segment> segments;

    public TrainInfoAdapter(List<YandexRaspResponse.Segment> segments) {
        this.segments = segments;
    }

    @NonNull
    @Override
    public TrainInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_train_info, parent, false);
        return new TrainInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrainInfoViewHolder holder, int position) {
        YandexRaspResponse.Segment segment = segments.get(position);
        holder.trainNumber.setText(segment.getThread().getNumber());
        holder.departureTime.setText("Отправление: " + segment.getDeparture().substring(11, 16));
        holder.arrivalTime.setText("Прибытие: " + segment.getArrival().substring(11, 16));
        holder.duration.setText("Длительность: " + segment.getDuration() / 60 + " мин");
    }

    @Override
    public int getItemCount() {
        return segments.size();
    }

    static class TrainInfoViewHolder extends RecyclerView.ViewHolder {
        TextView trainNumber;
        TextView departureTime;
        TextView arrivalTime;
        TextView duration;

        TrainInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            trainNumber = itemView.findViewById(R.id.trainNumber);
            departureTime = itemView.findViewById(R.id.departureTime);
            arrivalTime = itemView.findViewById(R.id.arrivalTime);
            duration = itemView.findViewById(R.id.duration);
        }
    }
}