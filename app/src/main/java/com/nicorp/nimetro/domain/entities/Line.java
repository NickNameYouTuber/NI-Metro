package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Line implements Parcelable {
    private String id;
    private String name;
    private String color;
    private List<Station> stations;
    private boolean isCircle;
    private String lineType;

    public Line(String id, String name, String color, boolean isCircle, String lineType) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isCircle = isCircle;
        this.stations = new ArrayList<>();
        this.lineType = lineType;
    }

    protected Line(Parcel in) {
        id = in.readString();
        name = in.readString();
        color = in.readString();
        stations = in.createTypedArrayList(Station.CREATOR);
        isCircle = in.readByte() != 0;
        lineType = in.readString();
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
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(color);
        dest.writeTypedList(stations);
        dest.writeByte((byte) (isCircle ? 1 : 0));
        dest.writeString(lineType);
    }

    public String getId() {
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

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getLineIdForStation(Station station) {
        Log.d("Line", "Searching for station in this line " + station.getName());
        if (stations.contains(station)) {
            return id;
        }
        return null;
    }
}