package com.nicorp.nimetro.api.controllers;

import com.nicorp.nimetro.api.dto.NotificationResponse;
import com.nicorp.nimetro.api.entities.NotificationEntity;
import com.nicorp.nimetro.api.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String lineId) {
        
        List<NotificationEntity> notifications;
        
        if (stationId != null && !stationId.isBlank()) {
            notifications = notificationService.getNotificationsByStationId(stationId);
        } else if (lineId != null && !lineId.isBlank()) {
            notifications = notificationService.getNotificationsByLineId(lineId);
        } else {
            notifications = notificationService.getActiveNotificationsForToday(stationId, lineId);
        }
        
        List<NotificationResponse> response = notifications.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable String id) {
        return notificationService.getNotificationById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@RequestBody NotificationEntity notification) {
        NotificationEntity created = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponse> updateNotification(@PathVariable String id, @RequestBody NotificationEntity notification) {
        return notificationService.updateNotification(id, notification)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        if (notificationService.deleteNotification(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
            .id(entity.getId())
            .type(entity.getType())
            .triggerType(entity.getTriggerType())
            .triggerStationId(entity.getTriggerStationId())
            .triggerLineId(entity.getTriggerLineId())
            .triggerDateStart(entity.getTriggerDateStart())
            .triggerDateEnd(entity.getTriggerDateEnd())
            .contentText(entity.getContentText())
            .contentImageUrl(entity.getContentImageUrl())
            .contentImageResource(entity.getContentImageResource())
            .contentCaption(entity.getContentCaption())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .isActive(entity.getIsActive())
            .build();
    }
}

