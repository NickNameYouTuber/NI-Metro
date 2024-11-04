package com.nicorp.nimetro;

import android.graphics.Point;

public class MapObject {
    private String name;
    private String displayName;
    private String type;
    private Point position;

    public MapObject(String name, String type, Point position, String displayName) {
        this.name = name;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return displayName;
    }
}