package com.miniCalendly.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.miniCalendly.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleCalendarService {

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private UserRepository userRepository;

    private Calendar getCalendarService(String hostEmail) throws Exception {
        System.out.println("DEBUG: Looking up Google OAuth2 client for host: " + hostEmail);
        
        String principalName = hostEmail;
        com.miniCalendly.model.User user = userRepository.findByUsername(hostEmail).orElse(null);
        if (user != null && user.getGoogleSub() != null) {
            principalName = user.getGoogleSub();
            System.out.println("DEBUG: Found Google Sub ID in DB: " + principalName);
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", principalName);
        
        if (client == null && !principalName.equals(hostEmail)) {
            System.out.println("DEBUG: Lookup by Sub ID failed. Falling back to email lookup for " + hostEmail);
            client = authorizedClientService.loadAuthorizedClient("google", hostEmail);
        }
        
        if (client == null) {
            System.out.println("DEBUG: Client not found in service. Attempting manager fallback.");
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("google")
                    .principal(new org.springframework.security.authentication.TestingAuthenticationToken(principalName, null))
                    .build();
            client = authorizedClientManager.authorize(authorizeRequest);
        }

        if (client == null) {
            System.err.println("DEBUG: NO AUTHORIZED CLIENT FOUND FOR " + hostEmail + " (searched using " + principalName + ")");
            throw new Exception("No Google Calendar connection found for user: " + hostEmail);
        }
        
        System.out.println("DEBUG: Successfully retrieved Google OAuth2 client for " + hostEmail);
        OAuth2AccessToken accessToken = client.getAccessToken();
        
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken.getTokenValue()))
                .setApplicationName("Mini Calendly")
                .build();
    }

    /** Creates a single Google Calendar event. */
    public void createCalendarEvent(String hostEmail, String guestEmail, String title,
                                     String description, String dateTime, int durationMinutes, String location) {
        try {
            Calendar service = getCalendarService(hostEmail);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime startLocal = LocalDateTime.parse(dateTime, formatter);
            LocalDateTime endLocal = startLocal.plusMinutes(durationMinutes);

            String userTz = resolveTimezone(hostEmail);
            ZoneId hostZone = ZoneId.of(userTz);

            Event event = buildEventBase(title, description, location, hostEmail);

            ZonedDateTime zonedStart = startLocal.atZone(hostZone);
            ZonedDateTime zonedEnd = endLocal.atZone(hostZone);

            String startIso = zonedStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String endIso = zonedEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            event.setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startIso)).setTimeZone(userTz));
            event.setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endIso)).setTimeZone(userTz));
            
            List<EventAttendee> attendees = new ArrayList<>();
            // Host as attendee (accepted)
            attendees.add(new EventAttendee().setEmail(hostEmail).setResponseStatus("accepted").setOrganizer(true));
            // Guest as attendee
            attendees.add(new EventAttendee().setEmail(guestEmail));
            event.setAttendees(attendees);

            service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();
            
            System.out.println("DEBUG: Single event created in Google Calendar for " + hostEmail + " with guest " + guestEmail);

        } catch (Exception e) {
            System.err.println("Failed to create Google Calendar event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a recurring weekday (Mon–Fri) Google Calendar event covering the entire date range.
     * Used when a host accepts a booking on a recurring (date-range) event type.
     *
     * @param endDate last date of recurrence (inclusive), format yyyy-MM-dd
     */
    public void createRecurringCalendarEvent(String hostEmail, String guestEmail, String title,
                                              String description, String dateTime, int durationMinutes,
                                              String location, String endDate) {
        try {
            Calendar service = getCalendarService(hostEmail);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime startLocal = LocalDateTime.parse(dateTime, formatter);
            LocalDateTime endLocal = startLocal.plusMinutes(durationMinutes);

            String userTz = resolveTimezone(hostEmail);
            ZoneId hostZone = ZoneId.of(userTz);

            Event event = buildEventBase(title, description, location, hostEmail);

            ZonedDateTime zonedStart = startLocal.atZone(hostZone);
            ZonedDateTime zonedEnd = endLocal.atZone(hostZone);

            String startIso = zonedStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String endIso = zonedEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            event.setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startIso)).setTimeZone(userTz));
            event.setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endIso)).setTimeZone(userTz));

            // RRULE: repeat every Mon–Fri until the endDate (inclusive)
            String untilDate = endDate.replace("-", "") + "T235959Z";
            event.setRecurrence(Collections.singletonList(
                "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;UNTIL=" + untilDate
            ));

            List<EventAttendee> attendees = new ArrayList<>();
            // Host as attendee (accepted)
            attendees.add(new EventAttendee().setEmail(hostEmail).setResponseStatus("accepted").setOrganizer(true));
            // Guest as attendee
            attendees.add(new EventAttendee().setEmail(guestEmail));
            event.setAttendees(attendees);

            service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            System.out.println("DEBUG: Recurring event (Mon-Fri until " + endDate + ") created for " + hostEmail);

        } catch (Exception e) {
            System.err.println("Failed to create recurring Google Calendar event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveTimezone(String hostEmail) {
        com.miniCalendly.model.User host = userRepository.findByUsername(hostEmail).orElse(null);
        if (host != null && host.getTime_zone() != null && !host.getTime_zone().isEmpty()) {
            return host.getTime_zone();
        }
        return "Asia/Kolkata";
    }

    private Event buildEventBase(String title, String description, String location, String hostEmail) {
        Event event = new Event().setSummary(title).setDescription(description);
        
        // Explicitly set organizer
        Event.Organizer organizer = new Event.Organizer();
        organizer.setEmail(hostEmail);
        event.setOrganizer(organizer);
        
        // Guest permissions
        event.setGuestsCanInviteOthers(false);
        event.setGuestsCanSeeOtherGuests(true);
        
        if ("Google Meet".equals(location)) {
            ConferenceData conferenceData = new ConferenceData();
            CreateConferenceRequest createRequest = new CreateConferenceRequest()
                    .setRequestId(UUID.randomUUID().toString())
                    .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"));
            conferenceData.setCreateRequest(createRequest);
            event.setConferenceData(conferenceData);
        } else if (location != null && !location.isEmpty()) {
            event.setLocation(location);
        }
        return event;
    }
}
