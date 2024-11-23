package com.nicorp.nimetro.domain.entities;

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
}