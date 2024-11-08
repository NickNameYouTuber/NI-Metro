package com.nicorp.nimetro.domain.usecases;

import com.nicorp.nimetro.domain.entities.Route;
import com.nicorp.nimetro.data.repositories.RouteRepository;

public class GetRouteInfoUseCase {
    private final RouteRepository routeRepository;

    public GetRouteInfoUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public Route execute(int startStationId, int endStationId) {
        return routeRepository.getRoute(startStationId, endStationId);
    }
}