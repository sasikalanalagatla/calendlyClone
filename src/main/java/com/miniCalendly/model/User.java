package com.miniCalendly.model;

import com.miniCalendly.util.GetTimeStamp;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name="calendly_users")
public class User {
    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String name;
    private String time_zone;
    private String googleMeetLink;
    private String googleSub;
    private String profilePicture;
    private String welcomeMessage;
    private String language = "English";
    private String dateFormat = "DD/MM/YYYY";
    private String timeFormat = "24h";
    private String country = "India";
    private boolean calendarPromptDone; // true = user has already been asked about Google Calendar

    private Timestamp created_at;
    private Timestamp modified_at;


    public User() {}

    public User(String username, String name, String time_zone) {
        this.username = username;
        this.name = name;
        this.time_zone = time_zone;
        this.created_at = new GetTimeStamp().getTimestamp();
        this.modified_at = new GetTimeStamp().getTimestamp();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime_zone() { return time_zone; }
    public void setTime_zone(String time_zone) { this.time_zone = time_zone; }

    public String getGoogleMeetLink() { return googleMeetLink; }
    public void setGoogleMeetLink(String googleMeetLink) { this.googleMeetLink = googleMeetLink; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public Timestamp getModified_at() { return modified_at; }
    public void setModified_at(Timestamp modified_at) { this.modified_at = modified_at; }

    public boolean isCalendarPromptDone() { return calendarPromptDone; }
    public void setCalendarPromptDone(boolean calendarPromptDone) { this.calendarPromptDone = calendarPromptDone; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}


