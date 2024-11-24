package com.nicorp.nimetro.data.models;

import android.graphics.Point;

public class MapObject {
    private String name;
    private String displayNumber;
    private String type;
    private Point position;

    public MapObject(String name, String type, Point position, String displayNumber) {
        this.name = name;
        this.displayNumber = displayNumber;
        this.type = type;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Point getPosition() {
        return position;
    }

    public String getdisplayNumber() {
        return displayNumber;
    }
}