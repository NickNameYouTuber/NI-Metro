package com.nicorp.nimetro.domain.entities;

import java.util.Objects;

public class RouteStation {
    private Station station;
    private Line line;
    
    public RouteStation(Station station, Line line) {
        this.station = station;
        this.line = line;
    }
    
    public Station getStation() {
        return station;
    }
    
    public Line getLine() {
        return line;
    }
    
    public void setLine(Line line) {
        this.line = line;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteStation that = (RouteStation) o;
        return Objects.equals(station, that.station) && Objects.equals(line, that.line);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(station, line);
    }
}

