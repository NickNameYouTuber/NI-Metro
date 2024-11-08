package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Facilities implements Parcelable {
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

    protected Facilities(Parcel in) {
        schedule = in.readString();
        escalators = in.readInt();
        elevators = in.readInt();
        exits = in.createStringArray();
    }

    public static final Creator<Facilities> CREATOR = new Creator<Facilities>() {
        @Override
        public Facilities createFromParcel(Parcel in) {
            return new Facilities(in);
        }

        @Override
        public Facilities[] newArray(int size) {
            return new Facilities[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(schedule);
        dest.writeInt(escalators);
        dest.writeInt(elevators);
        dest.writeStringArray(exits);
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