// com/nicorp/nimetro/data/models/StationDto.java
package com.nicorp.nimetro.data.models;

import com.nicorp.nimetro.domain.entities.Facilities;

public class StationDto {
    private int id;
    private String name;
    private int x;
    private int y;
    private String color;
    private Facilities facilities;
    private int textPosition;
    private Integer labelX;
    private Integer labelY;

    public StationDto(int id, String name, int x, int y, String color, Facilities facilities, int textPosition) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color;
        this.facilities = facilities;
        this.textPosition = textPosition;
        this.labelX = null;
        this.labelY = null;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Facilities getFacilities() {
        return facilities;
    }

    public void setFacilities(Facilities facilities) {
        this.facilities = facilities;
    }

    public int getTextPosition() {
        return textPosition;
    }

    public void setTextPosition(int textPosition) {
        this.textPosition = textPosition;
    }

    public Integer getLabelX() {
        return labelX;
    }

    public void setLabelX(Integer labelX) {
        this.labelX = labelX;
    }

    public Integer getLabelY() {
        return labelY;
    }

    public void setLabelY(Integer labelY) {
        this.labelY = labelY;
    }
}