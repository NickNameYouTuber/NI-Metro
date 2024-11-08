package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Route implements Parcelable {
    private List<Station> stations;

    public Route(List<Station> stations) {
        this.stations = stations;
    }

    protected Route(Parcel in) {
        stations = in.createTypedArrayList(Station.CREATOR);
    }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        @Override
        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(stations);
    }

    public List<Station> getStations() {
        return stations;
    }
}