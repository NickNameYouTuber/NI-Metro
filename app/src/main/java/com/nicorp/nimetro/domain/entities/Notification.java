package com.nicorp.nimetro.domain.entities;

public class Notification {
    public enum NotificationType {
        NORMAL,
        IMPORTANT
    }

    private String id;
    private NotificationType type;
    private NotificationTrigger trigger;
    private NotificationContent content;

    public Notification() {
    }

    public Notification(String id, NotificationType type, NotificationTrigger trigger, NotificationContent content) {
        this.id = id;
        this.type = type;
        this.trigger = trigger;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(NotificationTrigger trigger) {
        this.trigger = trigger;
    }

    public NotificationContent getContent() {
        return content;
    }

    public void setContent(NotificationContent content) {
        this.content = content;
    }

    public static class NotificationContent {
        private String text;
        private String imageUrl;
        private String imageResource;
        private String caption;

        public NotificationContent() {
        }

        public NotificationContent(String text) {
            this.text = text;
        }

        public NotificationContent(String imageUrl, String imageResource, String caption) {
            this.imageUrl = imageUrl;
            this.imageResource = imageResource;
            this.caption = caption;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getImageResource() {
            return imageResource;
        }

        public void setImageResource(String imageResource) {
            this.imageResource = imageResource;
        }

        public String getCaption() {
            return caption;
        }

        public void setCaption(String caption) {
            this.caption = caption;
        }

        public boolean hasImage() {
            return (imageUrl != null && !imageUrl.isEmpty()) || 
                   (imageResource != null && !imageResource.isEmpty());
        }
    }
}

