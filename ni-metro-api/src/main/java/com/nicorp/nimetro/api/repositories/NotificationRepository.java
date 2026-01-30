package com.nicorp.nimetro.api.repositories;

import com.nicorp.nimetro.api.entities.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    
    List<NotificationEntity> findByIsActiveTrue();
    
    Optional<NotificationEntity> findByIdAndIsActiveTrue(String id);
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.isActive = true " +
           "AND (n.triggerType = 'once' OR " +
           "(n.triggerType = 'date_range' AND n.triggerDateStart <= :today AND n.triggerDateEnd >= :today) OR " +
           "(n.triggerType = 'station' AND n.triggerStationId = :stationId AND " +
           "(n.triggerDateStart IS NULL OR (n.triggerDateStart <= :today AND n.triggerDateEnd >= :today))) OR " +
           "(n.triggerType = 'line' AND n.triggerLineId = :lineId AND " +
           "(n.triggerDateStart IS NULL OR (n.triggerDateStart <= :today AND n.triggerDateEnd >= :today))))")
    List<NotificationEntity> findActiveNotificationsForToday(
        @Param("today") LocalDate today,
        @Param("stationId") String stationId,
        @Param("lineId") String lineId
    );
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.isActive = true " +
           "AND n.triggerType = 'station' AND n.triggerStationId = :stationId " +
           "AND (n.triggerDateStart IS NULL OR (n.triggerDateStart <= :today AND n.triggerDateEnd >= :today))")
    List<NotificationEntity> findByStationId(@Param("stationId") String stationId, @Param("today") LocalDate today);
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.isActive = true " +
           "AND n.triggerType = 'line' AND n.triggerLineId = :lineId " +
           "AND (n.triggerDateStart IS NULL OR (n.triggerDateStart <= :today AND n.triggerDateEnd >= :today))")
    List<NotificationEntity> findByLineId(@Param("lineId") String lineId, @Param("today") LocalDate today);
}

