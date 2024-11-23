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
        private TicketsInfo tickets_info;

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

        public TicketsInfo getTicketsInfo() {
            return tickets_info;
        }

        // Parcelable implementation
        protected Segment(Parcel in) {
            thread = in.readParcelable(Thread.class.getClassLoader());
            departure = in.readString();
            arrival = in.readString();
            duration = in.readDouble();
            tickets_info = in.readParcelable(TicketsInfo.class.getClassLoader());
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
            dest.writeParcelable(tickets_info, flags);
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

    public static class TicketsInfo implements Parcelable {
        private boolean et_marker;
        private List<Place> places;

        public boolean isEtMarker() {
            return et_marker;
        }

        public List<Place> getPlaces() {
            return places;
        }

        // Parcelable implementation
        protected TicketsInfo(Parcel in) {
            et_marker = in.readByte() != 0;
            places = in.createTypedArrayList(Place.CREATOR);
        }

        public TicketsInfo() {
            // Default constructor
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (et_marker ? 1 : 0));
            dest.writeTypedList(places);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TicketsInfo> CREATOR = new Creator<TicketsInfo>() {
            @Override
            public TicketsInfo createFromParcel(Parcel in) {
                return new TicketsInfo(in);
            }

            @Override
            public TicketsInfo[] newArray(int size) {
                return new TicketsInfo[size];
            }
        };

        public static class Place implements Parcelable {
            private String name;
            private Price price;
            private String currency;

            public String getName() {
                return name;
            }

            public Price getPrice() {
                return price;
            }

            public String getCurrency() {
                return currency;
            }

            // Parcelable implementation
            protected Place(Parcel in) {
                name = in.readString();
                price = in.readParcelable(Price.class.getClassLoader());
                currency = in.readString();
            }

            public Place() {
                // Default constructor
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(name);
                dest.writeParcelable(price, flags);
                dest.writeString(currency);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Place> CREATOR = new Creator<Place>() {
                @Override
                public Place createFromParcel(Parcel in) {
                    return new Place(in);
                }

                @Override
                public Place[] newArray(int size) {
                    return new Place[size];
                }
            };
        }

        public static class Price implements Parcelable {
            private int whole;
            private int cents;

            public int getWhole() {
                return whole;
            }

            public int getCents() {
                return cents;
            }

            // Parcelable implementation
            protected Price(Parcel in) {
                whole = in.readInt();
                cents = in.readInt();
            }

            public Price() {
                // Default constructor
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(whole);
                dest.writeInt(cents);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Price> CREATOR = new Creator<Price>() {
                @Override
                public Price createFromParcel(Parcel in) {
                    return new Price(in);
                }

                @Override
                public Price[] newArray(int size) {
                    return new Price[size];
                }
            };
        }
    }
}