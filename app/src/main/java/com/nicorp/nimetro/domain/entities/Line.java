package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Line implements Parcelable {
    private int id;
    private String name;
    private String color;
    private List<Station> stations;
    private boolean isCircle;

    public Line(int id, String name, String color, boolean isCircle) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isCircle = isCircle;
        this.stations = new ArrayList<>();
    }

    protected Line(Parcel in) {
        id = in.readInt();
        name = in.readString();
        color = in.readString();
        stations = in.createTypedArrayList(Station.CREATOR);
        isCircle = in.readByte() != 0;
    }

    public static final Creator<Line> CREATOR = new Creator<Line>() {
        @Override
        public Line createFromParcel(Parcel in) {
            return new Line(in);
        }

        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(color);
        dest.writeTypedList(stations);
        dest.writeByte((byte) (isCircle ? 1 : 0));
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public List<Station> getStations() {
        return stations;
    }

    public boolean isCircle() {
        return isCircle;
    }

    public int getLineIdForStation(Station station) {
        Log.d("Line", "Searching for station in this line " + station.getName());
        Log.d("Line", "Stations in this line: " + stations);
        if (stations.contains(station)) {
            Log.d("Line", "Found station in this line " + station.getName());
            Log.d("Line", "Line ID: " + id);
            return id;
        }
        return -1;
    }
}