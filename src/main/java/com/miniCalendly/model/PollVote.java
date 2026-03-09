package com.miniCalendly.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class PollVote {
    @Id
    @GeneratedValue
    private Long id;

    private Long pollOptionId;
    private String inviteeEmail;
    private String inviteeName;
    private String status = "AVAILABLE"; // AVAILABLE, NOT_AVAILABLE

    public PollVote() {}

    public PollVote(Long pollOptionId, String inviteeEmail, String inviteeName, String status) {
        this.pollOptionId = pollOptionId;
        this.inviteeEmail = inviteeEmail;
        this.inviteeName = inviteeName;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPollOptionId() { return pollOptionId; }
    public void setPollOptionId(Long pollOptionId) { this.pollOptionId = pollOptionId; }

    public String getInviteeEmail() { return inviteeEmail; }
    public void setInviteeEmail(String inviteeEmail) { this.inviteeEmail = inviteeEmail; }

    public String getInviteeName() { return inviteeName; }
    public void setInviteeName(String inviteeName) { this.inviteeName = inviteeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
