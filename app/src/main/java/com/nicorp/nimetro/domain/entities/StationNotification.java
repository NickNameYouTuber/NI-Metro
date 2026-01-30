package com.nicorp.nimetro.domain.entities;

public class StationNotification {
    public enum NotificationType {
        NORMAL,
        IMPORTANT
    }

    private NotificationType type;
    private String text;

    public StationNotification() {
    }

    public StationNotification(NotificationType type, String text) {
        this.type = type;
        this.text = text;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

