package com.miniCalendly.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class EventType {
    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private int durationMinutes;
    private String description;
    private String color;
    private Long userId;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String type = "ONE_ON_ONE";
    private int maxParticipants = 1;
    private int currentParticipants = 0;
    private String location;
    private String locationDetails;
    private boolean booked = false;

    public EventType() {}

    public EventType(String title, String description, int durationMinutes, Long userId, String color, String startDate, String endDate, String startTime, String endTime, String type, int maxParticipants, String location, String locationDetails) {
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.userId = userId;
        this.color = color;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.maxParticipants = maxParticipants;
        this.location = location;
        this.locationDetails = locationDetails;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLocationDetails() { return locationDetails; }
    public void setLocationDetails(String locationDetails) { this.locationDetails = locationDetails; }

    public boolean isBooked() { 
        if (booked) return true;
        return currentParticipants >= maxParticipants; 
    }
    public void setBooked(boolean booked) { this.booked = booked; }
}
