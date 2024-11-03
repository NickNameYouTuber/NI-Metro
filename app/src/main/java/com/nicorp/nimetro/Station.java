package com.nicorp.nimetro;

import android.graphics.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Station implements Serializable {
    private int id;
    private String name;
    private int x;
    private int y;
    private String color;
    private Facilities facilities;
    private List<Neighbor> neighbors;
    private int textPosition;

    public Station(int id, String name, int x, int y, String color, Facilities facilities, int textPosition) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color;
        this.facilities = facilities;
        this.neighbors = new ArrayList<>();
        this.textPosition = textPosition;
    }

    public int getId() {
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

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Neighbor neighbor) {
        neighbors.add(neighbor);
    }

    public int getTextPosition() {
        return textPosition;
    }

    public static class Neighbor implements Serializable {
        private Station station;
        private int time;

        public Neighbor(Station station, int time) {
            this.station = station;
            this.time = time;
        }

        public Station getStation() {
            return station;
        }

        public int getTime() {
            return time;
        }

        @Override
        public String toString() {
            return "Neighbor{" +
                    "station=" + station +
                    ", time=" + time +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Station{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", color='" + color + '\'' +
                ", textPosition=" + textPosition +
                '}';
    }

    private Map<Station, List<Point>> intermediatePointsMap = new HashMap<>();

    public void addIntermediatePoints(Station neighbor, List<Point> points) {
        intermediatePointsMap.put(neighbor, points);
    }

    public List<Point> getIntermediatePoints(Station neighbor) {
        return intermediatePointsMap.get(neighbor);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}