package com.miniCalendly.model;

import com.miniCalendly.util.GetTimeStamp;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
public class Booking {
    @Id
    @GeneratedValue
    private Long id;

    private Long mentorId;
    private Long userId;
    private Long eventTypeId;
    private String message;
    private String date_time;
    private String status = "PENDING";
    private String location;
    private String locationDetails;
    private Timestamp created_at;
    private Timestamp modified_at;

    public Booking() {}

    public Booking(Long mentorId, Long userId, Long eventTypeId, String message, String date_time, String location, String locationDetails) {
        this.mentorId = mentorId;
        this.userId = userId;
        this.eventTypeId = eventTypeId;
        this.message = message;
        this.date_time = date_time;
        this.location = location;
        this.locationDetails = locationDetails;
        this.created_at = new GetTimeStamp().getTimestamp();
        this.modified_at = new GetTimeStamp().getTimestamp();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDate_time() { return date_time; }
    public void setDate_time(String date_time) { this.date_time = date_time; }

    public Long getEventTypeId() { return eventTypeId; }
    public void setEventTypeId(Long eventTypeId) { this.eventTypeId = eventTypeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLocationDetails() { return locationDetails; }
    public void setLocationDetails(String locationDetails) { this.locationDetails = locationDetails; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public Timestamp getModified_at() { return modified_at; }
    public void setModified_at(Timestamp modified_at) { this.modified_at = modified_at; }
}

