package com.nicorp.nimetro.api.services;

import com.nicorp.nimetro.api.entities.MapEntity;
import com.nicorp.nimetro.api.repositories.MapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MapService {
    
    private final MapRepository mapRepository;
    
    public List<MapEntity> getAllActiveMaps() {
        return mapRepository.findByIsActiveTrueOrderByUpdatedAtDesc();
    }
    
    public Optional<MapEntity> getMapById(UUID id) {
        return mapRepository.findByIdAndIsActiveTrue(id);
    }
    
    public Optional<MapEntity> getMapByFileName(String fileName) {
        return mapRepository.findByFileNameAndIsActiveTrue(fileName);
    }
    
    @Transactional
    public MapEntity createMap(MapEntity map) {
        if (map.getCreatedAt() == null) {
            map.setCreatedAt(LocalDateTime.now());
        }
        if (map.getUpdatedAt() == null) {
            map.setUpdatedAt(LocalDateTime.now());
        }
        if (map.getIsActive() == null) {
            map.setIsActive(true);
        }
        return mapRepository.save(map);
    }
    
    @Transactional
    public Optional<MapEntity> updateMap(UUID id, MapEntity updatedMap) {
        return mapRepository.findById(id).map(existing -> {
            existing.setName(updatedMap.getName());
            existing.setCountry(updatedMap.getCountry());
            existing.setVersion(updatedMap.getVersion());
            existing.setAuthor(updatedMap.getAuthor());
            existing.setIconUrl(updatedMap.getIconUrl());
            existing.setFileName(updatedMap.getFileName());
            existing.setData(updatedMap.getData());
            existing.setUpdatedAt(LocalDateTime.now());
            return mapRepository.save(existing);
        });
    }
    
    @Transactional
    public boolean deleteMap(UUID id) {
        return mapRepository.findById(id).map(map -> {
            map.setIsActive(false);
            map.setUpdatedAt(LocalDateTime.now());
            mapRepository.save(map);
            return true;
        }).orElse(false);
    }
    
    public boolean existsByFileName(String fileName) {
        return mapRepository.existsByFileName(fileName);
    }
}

