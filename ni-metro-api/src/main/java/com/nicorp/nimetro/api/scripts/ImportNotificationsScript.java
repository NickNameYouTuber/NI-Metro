package com.nicorp.nimetro.api.scripts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicorp.nimetro.api.entities.NotificationEntity;
import com.nicorp.nimetro.api.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Script to import notifications from JSON file.
 * Run with: java -jar app.jar --import.notifications.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportNotificationsScript implements CommandLineRunner {
    
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private static final String IMPORT_ENABLED_PROP = "import.notifications.enabled";
    private static final String NOTIFICATIONS_FILE = "../app/src/main/assets/raw/notifications.json";
    
    @Override
    public void run(String... args) {
        String importEnabled = System.getProperty(IMPORT_ENABLED_PROP);
        if (!"true".equalsIgnoreCase(importEnabled)) {
            log.info("Notification import is disabled. Set -Dimport.notifications.enabled=true to enable.");
            return;
        }
        
        log.info("Starting notification import from file: {}", NOTIFICATIONS_FILE);
        
        try {
            Path filePath = Paths.get(NOTIFICATIONS_FILE);
            if (!Files.exists(filePath)) {
                log.warn("Notifications file does not exist: {}", filePath);
                return;
            }
            
            String jsonContent = Files.readString(filePath);
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // Handle general_notifications array
            if (rootNode.has("general_notifications")) {
                JsonNode generalNotifications = rootNode.get("general_notifications");
                if (generalNotifications.isArray()) {
                    for (JsonNode notificationNode : generalNotifications) {
                        importNotification(notificationNode);
                    }
                }
            }
            
            log.info("Notification import completed.");
        } catch (Exception e) {
            log.error("Error during notification import", e);
        }
    }
    
    private void importNotification(JsonNode notificationNode) {
        try {
            String id = notificationNode.get("id").asText();
            
            // Check if already exists
            if (notificationRepository.existsById(id)) {
                log.info("Notification {} already exists, skipping", id);
                return;
            }
            
            NotificationEntity.NotificationType type = NotificationEntity.NotificationType.valueOf(
                notificationNode.get("type").asText().toUpperCase()
            );
            
            JsonNode triggerNode = notificationNode.get("trigger");
            NotificationEntity.TriggerType triggerType = NotificationEntity.TriggerType.valueOf(
                triggerNode.get("type").asText().toUpperCase()
            );
            
            String triggerStationId = null;
            String triggerLineId = null;
            LocalDate triggerDateStart = null;
            LocalDate triggerDateEnd = null;
            
            if ("station".equals(triggerNode.get("type").asText())) {
                triggerStationId = triggerNode.get("station_id").asText();
                if (triggerNode.has("date_range")) {
                    JsonNode dateRange = triggerNode.get("date_range");
                    triggerDateStart = LocalDate.parse(dateRange.get("start").asText(), DATE_FORMATTER);
                    triggerDateEnd = LocalDate.parse(dateRange.get("end").asText(), DATE_FORMATTER);
                }
            } else if ("line".equals(triggerNode.get("type").asText())) {
                triggerLineId = triggerNode.get("line_id").asText();
                if (triggerNode.has("date_range")) {
                    JsonNode dateRange = triggerNode.get("date_range");
                    triggerDateStart = LocalDate.parse(dateRange.get("start").asText(), DATE_FORMATTER);
                    triggerDateEnd = LocalDate.parse(dateRange.get("end").asText(), DATE_FORMATTER);
                }
            } else if ("date_range".equals(triggerNode.get("type").asText())) {
                JsonNode dateRange = triggerNode.get("date_range");
                triggerDateStart = LocalDate.parse(dateRange.get("start").asText(), DATE_FORMATTER);
                triggerDateEnd = LocalDate.parse(dateRange.get("end").asText(), DATE_FORMATTER);
            }
            
            JsonNode contentNode = notificationNode.get("content");
            String contentText = contentNode.has("text") ? contentNode.get("text").asText() : null;
            String contentImageUrl = contentNode.has("image_url") ? contentNode.get("image_url").asText() : null;
            String contentImageResource = contentNode.has("image_resource") ? contentNode.get("image_resource").asText() : null;
            String contentCaption = contentNode.has("caption") ? contentNode.get("caption").asText() : null;
            
            NotificationEntity notification = NotificationEntity.builder()
                .id(id)
                .type(type)
                .triggerType(triggerType)
                .triggerStationId(triggerStationId)
                .triggerLineId(triggerLineId)
                .triggerDateStart(triggerDateStart)
                .triggerDateEnd(triggerDateEnd)
                .contentText(contentText)
                .contentImageUrl(contentImageUrl)
                .contentImageResource(contentImageResource)
                .contentCaption(contentCaption)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
            
            notificationRepository.save(notification);
            log.info("Successfully imported notification: {}", id);
            
        } catch (Exception e) {
            log.error("Error importing notification", e);
        }
    }
}

