package com.nicorp.nimetro.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nicorp.nimetro.api.entities.NotificationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private String id;
    private NotificationEntity.NotificationType type;
    private NotificationEntity.TriggerType triggerType;
    private String triggerStationId;
    private String triggerLineId;
    private LocalDate triggerDateStart;
    private LocalDate triggerDateEnd;
    private String contentText;
    private String contentImageUrl;
    private String contentImageResource;
    private String contentCaption;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}

