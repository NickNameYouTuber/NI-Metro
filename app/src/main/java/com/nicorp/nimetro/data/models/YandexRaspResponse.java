package com.nicorp.nimetro.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class YandexRaspResponse {
    private List<Segment> segments;

    public List<Segment> getSegments() {
        return segments;
    }

    public static class Segment implements Parcelable {
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

        // Parcelable implementation
        protected Segment(Parcel in) {
            thread = in.readParcelable(Thread.class.getClassLoader());
            departure = in.readString();
            arrival = in.readString();
            duration = in.readDouble();
        }

        public Segment() {
            // Default constructor
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(thread, flags);
            dest.writeString(departure);
            dest.writeString(arrival);
            dest.writeDouble(duration);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Segment> CREATOR = new Creator<Segment>() {
            @Override
            public Segment createFromParcel(Parcel in) {
                return new Segment(in);
            }

            @Override
            public Segment[] newArray(int size) {
                return new Segment[size];
            }
        };
    }

    public static class Thread implements Parcelable {
        private String number;
        private String title;
        private Carrier carrier;
        private String expressType;

        public String getExpressType() {
            return expressType;
        }

        public String getTitle() {
            return title;
        }

        public Carrier getCarrier() {
            return carrier;
        }

        public String getNumber() {
            return number;
        }

        // Parcelable implementation
        protected Thread(Parcel in) {
            number = in.readString();
        }

        public Thread() {
            // Default constructor
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(number);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Thread> CREATOR = new Creator<Thread>() {
            @Override
            public Thread createFromParcel(Parcel in) {
                return new Thread(in);
            }

            @Override
            public Thread[] newArray(int size) {
                return new Thread[size];
            }
        };
    }

    public static class Carrier implements Parcelable {
        private String title;

        public static final Creator<Carrier> CREATOR = new Creator<Carrier>() {
            @Override
            public Carrier createFromParcel(Parcel in) {
                return new Carrier(in);
            }

            @Override
            public Carrier[] newArray(int size) {
                return new Carrier[size];
            }
        };

        public String getTitle() {
            return title;
        }

        // Parcelable implementation
        protected Carrier(Parcel in) {
            title = in.readString();
        }

        public Carrier() {
            // Default constructor
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}