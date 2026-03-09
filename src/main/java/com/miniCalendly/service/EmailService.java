package com.miniCalendly.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendNotification(String to, String subject, String body) {
        if (mailSender == null) {
            System.out.println("DEBUG: Email Service not configured. Simulation only.");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    public void sendBookingRequestNotification(String hostEmail, String guestName, String dateTime) {
        String subject = "New Meeting Request: " + guestName;
        String body = "Hi,\n\n" + guestName + " has requested a meeting with you on " + dateTime + ".\n" +
                     "Please log in to your dashboard to Accept or Decline this request.\n\n" +
                     "Best,\nMini Calendly Team";
        sendNotification(hostEmail, subject, body);
    }

    public void sendBookingStatusNotification(String guestEmail, String hostName, String dateTime, String status) {
        String subject = "Meeting Request " + status + ": " + hostName;
        String body = "Hi,\n\nYour meeting request with " + hostName + " for " + dateTime + " has been " + status.toLowerCase() + ".\n\n" +
                     (status.equals("ACCEPTED") ? "See you then!" : "We're sorry, the host cannot make it at this time.") + "\n\n" +
                     "Best,\nMini Calendly Team";
        sendNotification(guestEmail, subject, body);
    }
}
