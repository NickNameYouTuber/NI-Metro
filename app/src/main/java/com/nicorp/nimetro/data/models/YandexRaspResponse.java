package com.nicorp.nimetro.data.models;

import java.util.List;

public class YandexRaspResponse {
    private List<Segment> segments;

    public List<Segment> getSegments() {
        return segments;
    }

    public static class Segment {
        private Thread thread;
        private String departure;
        private String arrival;
        private double duration;

        public Thread getThread() {
            return thread;
        }

        public String getDeparture() {
            return departure;
        }

        public String getArrival() {
            return arrival;
        }

        public double getDuration() {
            return duration;
        }
    }

    public static class Thread {
        private String number;

        public String getNumber() {
            return number;
        }
    }
}