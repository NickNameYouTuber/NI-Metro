package com.nicorp.nimetro.domain.entities;

public class NotificationTrigger {
    public enum TriggerType {
        ONCE,
        DATE_RANGE,
        STATION,
        LINE
    }

    private TriggerType type;
    private DateRange dateRange;
    private String stationId;
    private String lineId;

    public NotificationTrigger() {
    }

    public NotificationTrigger(TriggerType type, DateRange dateRange) {
        this.type = type;
        this.dateRange = dateRange;
    }

    public NotificationTrigger(TriggerType type, String stationId) {
        this.type = type;
        this.stationId = stationId;
    }

    public NotificationTrigger(TriggerType type, String lineId, DateRange dateRange) {
        this.type = type;
        this.lineId = lineId;
        this.dateRange = dateRange;
    }

    public TriggerType getType() {
        return type;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public static class DateRange {
        private String start;
        private String end;

        public DateRange() {
        }

        public DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }
}

