package com.nicorp.nimetro.data.api.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiNotification {
    @SerializedName("id")
    private String id;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("triggerType")
    private String triggerType;
    
    @SerializedName("triggerStationId")
    private String triggerStationId;
    
    @SerializedName("triggerLineId")
    private String triggerLineId;
    
    @SerializedName("triggerDateStart")
    private String triggerDateStart;
    
    @SerializedName("triggerDateEnd")
    private String triggerDateEnd;
    
    @SerializedName("contentText")
    private String contentText;
    
    @SerializedName("contentImageUrl")
    private String contentImageUrl;
    
    @SerializedName("contentImageResource")
    private String contentImageResource;
    
    @SerializedName("contentCaption")
    private String contentCaption;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;
    
    @SerializedName("isActive")
    private Boolean isActive;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerStationId() {
        return triggerStationId;
    }

    public void setTriggerStationId(String triggerStationId) {
        this.triggerStationId = triggerStationId;
    }

    public String getTriggerLineId() {
        return triggerLineId;
    }

    public void setTriggerLineId(String triggerLineId) {
        this.triggerLineId = triggerLineId;
    }

    public String getTriggerDateStart() {
        return triggerDateStart;
    }

    public void setTriggerDateStart(String triggerDateStart) {
        this.triggerDateStart = triggerDateStart;
    }

    public String getTriggerDateEnd() {
        return triggerDateEnd;
    }

    public void setTriggerDateEnd(String triggerDateEnd) {
        this.triggerDateEnd = triggerDateEnd;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentImageUrl() {
        return contentImageUrl;
    }

    public void setContentImageUrl(String contentImageUrl) {
        this.contentImageUrl = contentImageUrl;
    }

    public String getContentImageResource() {
        return contentImageResource;
    }

    public void setContentImageResource(String contentImageResource) {
        this.contentImageResource = contentImageResource;
    }

    public String getContentCaption() {
        return contentCaption;
    }

    public void setContentCaption(String contentCaption) {
        this.contentCaption = contentCaption;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

