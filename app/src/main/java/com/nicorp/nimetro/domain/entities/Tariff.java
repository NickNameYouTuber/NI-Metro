package com.nicorp.nimetro.domain.entities;

import com.nicorp.nimetro.domain.entities.RouteSegment;

public interface Tariff {
    void calculateCost(RouteSegment segment, TariffCallback callback);
}