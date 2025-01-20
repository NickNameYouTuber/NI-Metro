package com.nicorp.nimetro.domain.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import com.nicorp.nimetro.domain.entities.Facilities;

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
    private Map<Station, List<Point>> intermediatePoints;
    private double latitude; // Широта станции
    private double longitude; // Долгота станции

    // Метод для расчета расстояния между текущей станцией и местоположением пользователя
    public double distanceTo(double userLatitude, double userLongitude) {
        double earthRadius = 6371; // Радиус Земли в километрах

        double latDistance = Math.toRadians(userLatitude - this.latitude);
        double lonDistance = Math.toRadians(userLongitude - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(userLatitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Расстояние в километрах
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isVisible(Rect visibleRect) {
        return visibleRect.contains((int) x, (int) y);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getESP() {
        return ESP;
    }

    public void setESP(String ESP) {
        this.ESP = ESP;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Facilities getFacilities() {
        return facilities;
    }

    public void setFacilities(Facilities facilities) {
        this.facilities = facilities;
    }

    public int getTextPosition() {
        return textPosition;
    }

    public void setTextPosition(int textPosition) {
        this.textPosition = textPosition;
    }

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public Map<Station, List<Point>> getIntermediatePoints() {
        return intermediatePoints;
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


    public void setIntermediatePoints(Map<Station, List<Point>> intermediatePoints) {
        this.intermediatePoints = intermediatePoints;
    }

    // Constructor remains the same
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
        this.intermediatePoints = new HashMap<>();
    }

    protected Station(Parcel in) {
        id = in.readString();
        name = in.readString();
        x = in.readInt();
        y = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        ESP = in.readString();
        color = in.readString();
        facilities = in.readParcelable(Facilities.class.getClassLoader());
        textPosition = in.readInt();

        // Read only the essential neighbor data to avoid circular references
        int size = in.readInt();
        neighbors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String neighborId = in.readString();
            int time = in.readInt();
            // Create a temporary station with just the ID
            Station neighborStation = new Station(neighborId, "", 0, 0, "", "", null, 0);
            neighbors.add(new Neighbor(neighborStation, time));
        }

        // Initialize empty intermediatePoints map
        intermediatePoints = new HashMap<>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeInt(x);
        dest.writeInt(y);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(ESP);
        dest.writeString(color);
        dest.writeParcelable(facilities, flags);
        dest.writeInt(textPosition);

        // Write only essential neighbor data
        dest.writeInt(neighbors.size());
        for (Neighbor neighbor : neighbors) {
            dest.writeString(neighbor.getStation().getId());
            dest.writeInt(neighbor.getTime());
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.drawCircle(x, y, 10, paint); // Example: draw a circle for the station
    }

    public void addIntermediatePoints(Station station2, List<Point> points) {
        intermediatePoints.put(station2, points);
    }

    public void addNeighbor(Neighbor neighbor) {
        neighbors.add(neighbor);
    }


    public static class Neighbor implements Parcelable {
        private Station station;
        private int time;

        public Station getStation() {
            return station;
        }

        public void setStation(Station station) {
            this.station = station;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public Neighbor(Station station, int time) {
            this.station = station;
            this.time = time;
        }

        protected Neighbor(Parcel in) {
            // Read only essential station data
            String stationId = in.readString();
            station = new Station(stationId, "", 0, 0, "", "", null, 0);
            time = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            // Write only essential station data
            dest.writeString(station.getId());
            dest.writeInt(time);
        }

        @Override
        public int describeContents() {
            return 0;
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
    }

    // Rest of the getters and setters remain the same

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
}