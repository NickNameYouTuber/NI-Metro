// com/nicorp/nimetro/data/models/RouteDto.java
package com.nicorp.nimetro.data.models;

import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class RouteDto {
    private List<Station> stations;

    public RouteDto(List<Station> stations) {
        this.stations = stations;
    }

    // Getters and setters

    public List<Station> getStations() {
        return stations;
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
    }
}
