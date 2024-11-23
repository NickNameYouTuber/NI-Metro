package com.nicorp.nimetro.domain.entities;

import com.nicorp.nimetro.domain.entities.RouteSegment;

import java.util.Map;

public class ZoneBasedTariff implements Tariff {
    private Map<Integer, Double> zonePrices;

    public ZoneBasedTariff(Map<Integer, Double> zonePrices) {
        this.zonePrices = zonePrices;
    }

    @Override
    public void calculateCost(RouteSegment segment, TariffCallback callback) {
        int zone = segment.getZone();
        double price = zonePrices.getOrDefault(zone, 0.0);
        callback.onCostCalculated(price);
    }
}