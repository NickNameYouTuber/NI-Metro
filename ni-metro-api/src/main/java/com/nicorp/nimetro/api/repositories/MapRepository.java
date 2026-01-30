package com.nicorp.nimetro.api.repositories;

import com.nicorp.nimetro.api.entities.MapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MapRepository extends JpaRepository<MapEntity, UUID> {
    List<MapEntity> findByIsActiveTrueOrderByUpdatedAtDesc();
    
    Optional<MapEntity> findByIdAndIsActiveTrue(UUID id);
    
    Optional<MapEntity> findByFileNameAndIsActiveTrue(String fileName);
    
    boolean existsByFileName(String fileName);
    
    @Query("SELECT m FROM MapEntity m WHERE m.isActive = true ORDER BY m.updatedAt DESC")
    List<MapEntity> findAllActiveMaps();
}

