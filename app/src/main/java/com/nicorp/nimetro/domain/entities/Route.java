package com.nicorp.nimetro.domain.entities;

import java.io.Serializable;
import java.util.List;

public class Route implements Serializable {
    private List<Station> stations;

    public Route(List<Station> stations) {
        this.stations = stations;
    }

    public List<Station> getStations() {
        return stations;
    }
}