// com/nicorp/nimetro/domain/usecases/CalculateTransferRouteUseCase.java
package com.nicorp.nimetro.domain.usecases;

import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.data.repositories.RouteRepository;

public class CalculateTransferRouteUseCase {
    private final RouteRepository routeRepository;

    public CalculateTransferRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public Route execute(int startStationId, int endStationId) {
        return routeRepository.calculateTransferRoute(startStationId, endStationId);
    }
}