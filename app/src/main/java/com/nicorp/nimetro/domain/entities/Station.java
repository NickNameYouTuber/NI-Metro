package com.nicorp.nimetro.domain.entities;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Station implements Parcelable {
    private String id;
    private String name;
    private int x;
    private int y;
    private String ESP;
    private String color;
    private Facilities facilities;
    private int textPosition;
    private List<Neighbor> neighbors;
    private Map<Station, List<Point>> intermediatePoints; // Изменение структуры данных

    public Station(String id, String name, int x, int y, String ESP, String color, Facilities facilities, int textPosition) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.ESP = ESP;
        this.color = color;
        this.facilities = facilities;
        this.textPosition = textPosition;
        this.neighbors = new ArrayList<>();
        this.intermediatePoints = new HashMap<>(); // Инициализация новой структуры данных
    }

    protected Station(Parcel in) {
        id = in.readString();
        name = in.readString();
        x = in.readInt();
        y = in.readInt();
        ESP = in.readString();
        color = in.readString();
        facilities = in.readParcelable(Facilities.class.getClassLoader());
        textPosition = in.readInt();
        neighbors = in.createTypedArrayList(Neighbor.CREATOR);
        intermediatePoints = new HashMap<>();
        in.readMap(intermediatePoints, Station.class.getClassLoader());
    }

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
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
        dest.writeInt(x);
        dest.writeInt(y);
        dest.writeString(ESP);
        dest.writeString(color);
        dest.writeParcelable(facilities, flags);
        dest.writeInt(textPosition);
        dest.writeTypedList(neighbors);
        dest.writeMap(intermediatePoints);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getColor() {
        return color;
    }

    public Facilities getFacilities() {
        return facilities;
    }

    public int getTextPosition() {
        return textPosition;
    }

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Neighbor neighbor) {
        neighbors.add(neighbor);
    }

    public List<Point> getIntermediatePoints(Station otherStation) {
        if (intermediatePoints != null) {
            List<Point> points = intermediatePoints.get(otherStation);
            if (points != null) {
                return points;
            }
            // Check the reverse order
            points = otherStation.intermediatePoints.get(this);
            if (points != null) {
                return points;
            }
        }
        return null;
    }

    public void addIntermediatePoints(Station station2, List<Point> points) {
        intermediatePoints.put(station2, points);
    }

    public static class Neighbor implements Parcelable {
        private Station station;
        private int time;

        public Neighbor(Station station, int time) {
            this.station = station;
            this.time = time;
        }

        protected Neighbor(Parcel in) {
            station = in.readParcelable(Station.class.getClassLoader());
            time = in.readInt();
        }

        public static final Creator<Neighbor> CREATOR = new Creator<Neighbor>() {
            @Override
            public Neighbor createFromParcel(Parcel in) {
                return new Neighbor(in);
            }

            @Override
            public Neighbor[] newArray(int size) {
                return new Neighbor[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(station, flags);
            dest.writeInt(time);
        }

        public Station getStation() {
            return station;
        }

        public int getTime() {
            return time;
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setFacilities(Facilities facilities) {
        this.facilities = facilities;
    }

    public void setTextPosition(int textPosition) {
        this.textPosition = textPosition;
    }

    public void setNeighbors(List<Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public Map<Station, List<Point>> getIntermediatePoints() {
        return intermediatePoints;
    }

    public void setIntermediatePoints(Map<Station, List<Point>> intermediatePoints) {
        this.intermediatePoints = intermediatePoints;
    }

    public String getESP() {
        return ESP;
    }

    public void setESP(String ESP) {
        this.ESP = ESP;
    }

    @Override
    public String toString() {
        return "Station{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}