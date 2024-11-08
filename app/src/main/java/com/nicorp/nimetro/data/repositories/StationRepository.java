// com/nicorp/nimetro/data/repositories/StationRepository.java
package com.nicorp.nimetro.data.repositories;

import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public interface StationRepository {
    List<Station> getStationsByLine(int lineId);
}