package com.nicorp.nimetro.domain.entities;

import com.google.gson.JsonObject;
import com.nicorp.nimetro.domain.entities.RouteSegment;

public interface Tariff {
    void calculateCost(RouteSegment segment, TariffCallback callback);
    JsonObject getJson();
    String getName();
}