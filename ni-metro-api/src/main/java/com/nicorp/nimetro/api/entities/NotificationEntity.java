package com.nicorp.nimetro.api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(name = "trigger_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
    
    @Column(name = "trigger_station_id")
    private String triggerStationId;
    
    @Column(name = "trigger_line_id")
    private String triggerLineId;
    
    @Column(name = "trigger_date_start")
    private LocalDate triggerDateStart;
    
    @Column(name = "trigger_date_end")
    private LocalDate triggerDateEnd;
    
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;
    
    @Column(name = "content_image_url", length = 500)
    private String contentImageUrl;
    
    @Column(name = "content_image_resource")
    private String contentImageResource;
    
    @Column(name = "content_caption", columnDefinition = "TEXT")
    private String contentCaption;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    public enum NotificationType {
        normal, important
    }
    
    public enum TriggerType {
        once, date_range, station, line
    }
}

