package com.nicorp.nimetro;

import java.util.List;

public class Transfer {
    private List<Station> stations;
    private int time;
    private String type;

    public Transfer(List<Station> stations, int time, String type) {
        this.stations = stations;
        this.time = time;
        this.type = type;
    }

    public List<Station> getStations() {
        return stations;
    }

    public int getTime() {
        return time;
    }

    public String getType() {
        return type;
    }
}