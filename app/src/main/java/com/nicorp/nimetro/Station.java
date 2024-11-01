package com.nicorp.nimetro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Station implements Serializable {
    private int id;
    private String name;
    private int x;
    private int y;
    private String color;
    private Facilities facilities;
    private List<Station> neighbors;
    private int textPosition; // Добавляем параметр textPosition


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

    public int getTextPosition() {
        return textPosition;
    }

    public void setTextPosition(int textPosition) {
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

    public List<Station> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Station neighbor) {
        neighbors.add(neighbor);
    }
}