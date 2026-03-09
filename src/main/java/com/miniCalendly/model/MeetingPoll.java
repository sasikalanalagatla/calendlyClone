package com.miniCalendly.model;

import com.miniCalendly.util.GetTimeStamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
public class MeetingPoll {
    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String description;
    private Long hostId;
    private String status = "OPEN"; // OPEN, CLOSED
    private Timestamp created_at;

    public MeetingPoll() {}

    public MeetingPoll(String title, String description, Long hostId) {
        this.title = title;
        this.description = description;
        this.hostId = hostId;
        this.created_at = new GetTimeStamp().getTimestamp();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
}
