package com.nicorp.nimetro.data.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class YandexRaspResponse {
    @SerializedName("segments")
    private List<Segment> segments;

    public List<Segment> getSegments() {
        return segments;
    }

    public static class Segment {
        @SerializedName("arrival")
        private String arrival;

        @SerializedName("departure")
        private String departure;

        public String getArrival() {
            return arrival;
        }

        public String getDeparture() {
            return departure;
        }
    }
}