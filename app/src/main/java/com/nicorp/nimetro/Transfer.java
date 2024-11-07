package com.nicorp.nimetro;

import java.io.Serializable;
import java.util.List;

public class Transfer implements Serializable {
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

    @Override
    public String toString() {
        return "Transfer{" +
                "stations=" + stations +
                ", time=" + time +
                ", type='" + type + '\'' +
                '}';
    }
}