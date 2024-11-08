package com.nicorp.nimetro.domain.usecases;

import com.nicorp.nimetro.data.repositories.MapRepository;
import com.nicorp.nimetro.domain.entities.Line;

import java.util.List;

public class GetMetroLinesUseCase {
    private final MapRepository mapRepository;

    public GetMetroLinesUseCase(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    public List<Line> execute() {
        return mapRepository.getLines();
    }
}