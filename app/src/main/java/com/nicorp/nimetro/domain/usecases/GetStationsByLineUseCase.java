// com/nicorp/nimetro/domain/usecases/GetStationsByLineUseCase.java
package com.nicorp.nimetro.domain.usecases;

import com.nicorp.nimetro.data.repositories.StationRepository;
import com.nicorp.nimetro.domain.entities.Station;

import java.util.List;

public class GetStationsByLineUseCase {
    private final StationRepository stationRepository;

    public GetStationsByLineUseCase(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<Station> execute(int lineId) {
        return stationRepository.getStationsByLine(lineId);
    }
}
