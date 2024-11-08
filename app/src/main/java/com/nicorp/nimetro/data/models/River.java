// com/nicorp/nimetro/data/models/River.java
package com.nicorp.nimetro.data.models;

import android.graphics.Point;

import java.util.List;

public class River {
    private List<Point> points;
    private int width;

    public River(List<Point> points, int width) {
        this.points = points;
        this.width = width;
    }

    // Getters and setters

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}