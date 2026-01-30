package com.nicorp.nimetro.api.services;

import com.nicorp.nimetro.api.entities.NotificationEntity;
import com.nicorp.nimetro.api.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public List<NotificationEntity> getAllActiveNotifications() {
        return notificationRepository.findByIsActiveTrue();
    }
    
    public List<NotificationEntity> getActiveNotificationsForToday(String stationId, String lineId) {
        LocalDate today = LocalDate.now();
        return notificationRepository.findActiveNotificationsForToday(today, stationId, lineId);
    }
    
    public List<NotificationEntity> getNotificationsByStationId(String stationId) {
        LocalDate today = LocalDate.now();
        return notificationRepository.findByStationId(stationId, today);
    }
    
    public List<NotificationEntity> getNotificationsByLineId(String lineId) {
        LocalDate today = LocalDate.now();
        return notificationRepository.findByLineId(lineId, today);
    }
    
    public Optional<NotificationEntity> getNotificationById(String id) {
        return notificationRepository.findByIdAndIsActiveTrue(id);
    }
    
    @Transactional
    public NotificationEntity createNotification(NotificationEntity notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getUpdatedAt() == null) {
            notification.setUpdatedAt(LocalDateTime.now());
        }
        if (notification.getIsActive() == null) {
            notification.setIsActive(true);
        }
        return notificationRepository.save(notification);
    }
    
    @Transactional
    public Optional<NotificationEntity> updateNotification(String id, NotificationEntity updatedNotification) {
        return notificationRepository.findById(id).map(existing -> {
            existing.setType(updatedNotification.getType());
            existing.setTriggerType(updatedNotification.getTriggerType());
            existing.setTriggerStationId(updatedNotification.getTriggerStationId());
            existing.setTriggerLineId(updatedNotification.getTriggerLineId());
            existing.setTriggerDateStart(updatedNotification.getTriggerDateStart());
            existing.setTriggerDateEnd(updatedNotification.getTriggerDateEnd());
            existing.setContentText(updatedNotification.getContentText());
            existing.setContentImageUrl(updatedNotification.getContentImageUrl());
            existing.setContentImageResource(updatedNotification.getContentImageResource());
            existing.setContentCaption(updatedNotification.getContentCaption());
            existing.setUpdatedAt(LocalDateTime.now());
            return notificationRepository.save(existing);
        });
    }
    
    @Transactional
    public boolean deleteNotification(String id) {
        return notificationRepository.findById(id).map(notification -> {
            notification.setIsActive(false);
            notification.setUpdatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            return true;
        }).orElse(false);
    }
}

