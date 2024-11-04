package com.nicorp.nimetro;

import java.util.List;

public class MetroMapGroup {
    private String country;
    private List<MetroMapItem> metroMapItems;

    public MetroMapGroup(String country, List<MetroMapItem> metroMapItems) {
        this.country = country;
        this.metroMapItems = metroMapItems;
    }

    public String getCountry() {
        return country;
    }

    public List<MetroMapItem> getMetroMapItems() {
        return metroMapItems;
    }
}