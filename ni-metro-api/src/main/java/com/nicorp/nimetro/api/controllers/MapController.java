package com.nicorp.nimetro.api.controllers;

import com.nicorp.nimetro.api.dto.MapListItemResponse;
import com.nicorp.nimetro.api.dto.MapResponse;
import com.nicorp.nimetro.api.entities.MapEntity;
import com.nicorp.nimetro.api.services.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
public class MapController {
    
    private final MapService mapService;
    
    @GetMapping
    public ResponseEntity<List<MapListItemResponse>> getAllMaps() {
        List<MapListItemResponse> maps = mapService.getAllActiveMaps().stream()
            .map(this::toListItemResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(maps);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MapResponse> getMapById(@PathVariable UUID id) {
        return mapService.getMapById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-name/{fileName}")
    public ResponseEntity<MapResponse> getMapByFileName(@PathVariable String fileName) {
        return mapService.getMapByFileName(fileName)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<MapResponse> createMap(@RequestBody MapEntity map) {
        MapEntity created = mapService.createMap(map);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MapResponse> updateMap(@PathVariable UUID id, @RequestBody MapEntity map) {
        return mapService.updateMap(id, map)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMap(@PathVariable UUID id) {
        if (mapService.deleteMap(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    private MapResponse toResponse(MapEntity entity) {
        return MapResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .country(entity.getCountry())
            .version(entity.getVersion())
            .author(entity.getAuthor())
            .iconUrl(entity.getIconUrl())
            .fileName(entity.getFileName())
            .data(entity.getData())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .isActive(entity.getIsActive())
            .build();
    }
    
    private MapListItemResponse toListItemResponse(MapEntity entity) {
        return MapListItemResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .country(entity.getCountry())
            .version(entity.getVersion())
            .author(entity.getAuthor())
            .iconUrl(entity.getIconUrl())
            .fileName(entity.getFileName())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

