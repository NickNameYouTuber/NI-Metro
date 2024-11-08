// com/nicorp/nimetro/domain/usecases/GetMapObjectsUseCase.java
package com.nicorp.nimetro.domain.usecases;

import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.data.repositories.MapRepository;

import java.util.List;

public class GetMapObjectsUseCase {
    private final MapRepository mapRepository;

    public GetMapObjectsUseCase(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    public List<MapObject> execute() {
        return mapRepository.getMapObjects();
    }
}