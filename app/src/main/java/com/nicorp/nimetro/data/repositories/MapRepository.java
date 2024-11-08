package com.nicorp.nimetro.data.repositories;

import com.nicorp.nimetro.data.models.MapObject;
import com.nicorp.nimetro.domain.entities.Line;

import java.util.List;

public interface MapRepository {
    List<Line> getLines();
    List<MapObject> getMapObjects();
}