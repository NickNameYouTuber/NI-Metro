// com/nicorp/nimetro/data/repositories/RouteRepository.java
package com.nicorp.nimetro.data.repositories;

import com.nicorp.nimetro.domain.entities.Route;

public interface RouteRepository {
    Route getRoute(int startStationId, int endStationId);
    Route calculateTransferRoute(int startStationId, int endStationId);
}