package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Transfer implements Parcelable {
    private List<Station> stations;
    private int time;
    private String type;

    public Transfer(List<Station> stations, int time, String type) {
        this.stations = stations;
        this.time = time;
        this.type = type;
    }

    protected Transfer(Parcel in) {
        stations = in.createTypedArrayList(Station.CREATOR);
        time = in.readInt();
        type = in.readString();
    }

    public static final Creator<Transfer> CREATOR = new Creator<Transfer>() {
        @Override
        public Transfer createFromParcel(Parcel in) {
            return new Transfer(in);
        }

        @Override
        public Transfer[] newArray(int size) {
            return new Transfer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(stations);
        dest.writeInt(time);
        dest.writeString(type);
    }

    public List<Station> getStations() {
        return stations;
    }

    public int getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "stations=" + stations +
                ", time=" + time +
                ", type='" + type + '\'' +
                '}';
    }
}