package com.nicorp.nimetro.domain.entities;

import android.util.Log;

import java.util.List;
import java.util.Locale;

public class RouteSegment {
    private List<Station> stations;
    private Line line;
    private int zone;

    public RouteSegment(List<Station> stations, Line line, int zone) {
        this.stations = stations;
        this.line = line;
        this.zone = zone;
    }

    public List<Station> getStations() {
        return stations;
    }

    public Line getLine() {
        return line;
    }

    public int getZone() {
        return zone;
    }
}