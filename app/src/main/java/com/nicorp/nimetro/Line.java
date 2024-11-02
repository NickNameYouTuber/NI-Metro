package com.nicorp.nimetro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Line implements Serializable {
    private int id;
    private String name;
    private String color;
    private List<Station> stations;
    private boolean isCircle; // Добавляем параметр isCircle

    public Line(int id, String name, String color, boolean isCircle) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isCircle = isCircle;
        this.stations = new ArrayList<>();
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
        if (stations.contains(station)) {
            return id;
        }
        return -1; // Return -1 if the station is not found in this line
    }
}