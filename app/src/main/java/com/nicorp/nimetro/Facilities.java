package com.nicorp.nimetro;

import java.io.Serializable;

class Facilities implements Serializable {
    private String schedule;
    private int escalators;
    private int elevators;
    private String[] exits;

    public Facilities(String schedule, int escalators, int elevators, String[] exits) {
        this.schedule = schedule != null ? schedule : "5:30 - 0:00";
        this.escalators = escalators >= 0 ? escalators : 0;
        this.elevators = elevators >= 0 ? elevators : 0;
        this.exits = exits != null ? exits : new String[0];
    }

    public String getSchedule() {
        return schedule;
    }

    public int getEscalators() {
        return escalators;
    }

    public int getElevators() {
        return elevators;
    }

    public String[] getExits() {
        return exits;
    }
}