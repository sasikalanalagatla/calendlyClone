package com.miniCalendly.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class PollOption {
    @Id
    @GeneratedValue
    private Long id;

    private Long pollId;
    private String dateTime; // "yyyy-MM-dd HH:mm"
    private int durationMinutes;

    public PollOption() {}

    public PollOption(Long pollId, String dateTime, int durationMinutes) {
        this.pollId = pollId;
        this.dateTime = dateTime;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPollId() { return pollId; }
    public void setPollId(Long pollId) { this.pollId = pollId; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
