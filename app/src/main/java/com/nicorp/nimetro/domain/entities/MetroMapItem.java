package com.nicorp.nimetro.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class MetroMapItem implements Parcelable {
    private String country;
    private String name;
    private String iconUrl;
    private String fileName;

    public MetroMapItem(String country, String name, String iconUrl, String fileName) {
        this.country = country;
        this.name = name;
        this.iconUrl = iconUrl;
        this.fileName = fileName;
    }

    protected MetroMapItem(Parcel in) {
        country = in.readString();
        name = in.readString();
        iconUrl = in.readString();
        fileName = in.readString();
    }

    public static final Creator<MetroMapItem> CREATOR = new Creator<MetroMapItem>() {
        @Override
        public MetroMapItem createFromParcel(Parcel in) {
            return new MetroMapItem(in);
        }

        @Override
        public MetroMapItem[] newArray(int size) {
            return new MetroMapItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(country);
        dest.writeString(name);
        dest.writeString(iconUrl);
        dest.writeString(fileName);
    }

    public String getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "MetroMapItem{" +
                "country='" + country + '\'' +
                ", name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}