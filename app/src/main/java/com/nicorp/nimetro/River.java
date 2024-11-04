package com.nicorp.nimetro;

import android.graphics.Point;

import java.util.List;

public class River {
    private List<Point> points;
    private int width;

    public River(List<Point> points, int width) {
        this.points = points;
        this.width = width;
    }

    public List<Point> getPoints() {
        return points;
    }

    public int getWidth() {
        return width;
    }
}