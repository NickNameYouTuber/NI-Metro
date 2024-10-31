package com.nicorp.nimetro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Line implements Serializable {
    private int id;
    private String name;
    private String color;
    private List<Station> stations;

    public Line(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
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
}