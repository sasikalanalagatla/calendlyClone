package com.miniCalendly.model;

import com.miniCalendly.util.GetTimeStamp;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
public class Opening {
    @Id
    @GeneratedValue
    private Long id;

    private Long owner;
    private String date_time;
    private Timestamp created_at;
    private Timestamp modified_at;

    public Opening() {}

    public Opening(Long owner, String date_time) {
        this.owner = owner;
        this.date_time = date_time;
        this.created_at = new GetTimeStamp().getTimestamp();
        this.modified_at = new GetTimeStamp().getTimestamp();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwner() { return owner; }
    public void setOwner(Long owner) { this.owner = owner; }

    public String getDate_time() { return date_time; }
    public void setDate_time(String date_time) { this.date_time = date_time; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public Timestamp getModified_at() { return modified_at; }
    public void setModified_at(Timestamp modified_at) { this.modified_at = modified_at; }
}

