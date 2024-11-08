package com.nicorp.nimetro.domain.entities;

public class MetroMapItem {
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