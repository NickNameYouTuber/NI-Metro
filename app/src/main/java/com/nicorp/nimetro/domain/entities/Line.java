package com.nicorp.nimetro.domain.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.nicorp.nimetro.domain.entities.Tariff;

import java.util.ArrayList;
import java.util.List;

public class Line implements Parcelable {
    private String id;
    private String name;
    private String color;
    private List<Station> stations;
    private boolean isCircle;
    private String lineType;
    private Tariff tariff;
    private String displayNumber;
    private String displayShape;

    public Line(String id, String name, String color, boolean isCircle, String lineType, Tariff tariff, String displayNumber, String displayShape) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isCircle = isCircle;
        this.stations = new ArrayList<>();
        this.lineType = lineType;
        this.tariff = tariff;
        this.displayNumber = displayNumber;
        this.displayShape = displayShape;
    }

    protected Line(Parcel in) {
        id = in.readString();
        name = in.readString();
        color = in.readString();
        stations = in.createTypedArrayList(Station.CREATOR);
        isCircle = in.readByte() != 0;
        lineType = in.readString();
        tariff = null;
        displayNumber = in.readString();
        displayShape = in.readString();
        // Tariff не может быть просто так десериализован, нужно будет добавить логику для этого
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
        dest.writeString(displayNumber);
        dest.writeString(displayShape);
        // Tariff не может быть просто так сериализован, нужно будет добавить логику для этого
    }

    public boolean isVisible(Rect visibleRect) {
        for (Station station : stations) {
            if (station.isVisible(visibleRect)) {
                return true;
            }
        }
        return false;
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Station station : stations) {
            station.draw(canvas, paint);
        }
        // Draw the line connecting the stations
        for (int i = 0; i < stations.size() - 1; i++) {
            Station start = stations.get(i);
            Station end = stations.get(i + 1);
            canvas.drawLine(start.getX(), start.getY(), end.getX(), end.getY(), paint);
        }
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
//        Log.d("Line", "Searching for station in this line " + station.getName());
        if (stations.contains(station)) {
            return id;
        }
        return null;
    }

    public String getLineDisplayNumberForStation(Station station) {
        if (stations.contains(station)) {
            return displayNumber;
        }
        return null;
    }

    public Tariff getTariff() {
        return tariff;
    }

    public void setTariff(Tariff tariff) {
        this.tariff = tariff;
    }

    public String getdisplayNumber() {
        return displayNumber;
    }

    public void setdisplayNumber(String displayNumber) {
        this.displayNumber = displayNumber;
    }

    public String getDisplayShape() {
        return displayShape;
    }

    public boolean isAPITariffLine() {
        return tariff instanceof APITariff;
    }
}