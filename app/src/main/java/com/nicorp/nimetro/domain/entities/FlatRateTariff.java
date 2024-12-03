package com.nicorp.nimetro.domain.entities;

import com.google.gson.JsonObject;
import com.nicorp.nimetro.domain.entities.RouteSegment;

public class FlatRateTariff implements Tariff {
    private double price;

    public FlatRateTariff(double price) {
        this.price = price;
    }

    @Override
    public void calculateCost(RouteSegment segment, TariffCallback callback) {
        callback.onCostCalculated(price);
    }

    @Override
    public JsonObject getJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "FlatRateTariff");
        json.addProperty("price", price);

        return json;
    }

    @Override
    public String getName() {
        return "FlatRateTariff";
    }
}