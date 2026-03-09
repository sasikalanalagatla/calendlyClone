package com.miniCalendly.controller;

import com.miniCalendly.model.Booking;
import com.miniCalendly.model.EventType;
import com.miniCalendly.model.Opening;
import com.miniCalendly.model.MeetingPoll;
import com.miniCalendly.model.User;
import com.miniCalendly.repository.BookingRepository;
import com.miniCalendly.repository.EventTypeRepository;
import com.miniCalendly.repository.OpeningRepository;
import com.miniCalendly.repository.MeetingPollRepository;
import com.miniCalendly.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Controller
public class WebUIController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OpeningRepository openingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private MeetingPollRepository meetingPollRepository;

    @Autowired
    private com.miniCalendly.service.EmailService emailService;
    
    @Autowired
    private org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private com.miniCalendly.service.GoogleCalendarService googleCalendarService;

    @ModelAttribute
    public void addAttributes(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                model.addAttribute("loggedInUser", user);
                String principalName = user.getGoogleSub() != null ? user.getGoogleSub() : user.getUsername();
                boolean googleConnected = authorizedClientService.loadAuthorizedClient("google", principalName) != null;
                if (!googleConnected && user.getGoogleSub() != null) {
                    googleConnected = authorizedClientService.loadAuthorizedClient("google", user.getUsername()) != null;
                }
                model.addAttribute("googleConnected", googleConnected);
            });
        }
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirectUrl, Model model) {
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            model.addAttribute("redirectUrl", redirectUrl);
        }
        return "login";
    }

    @PostMapping("/login")
    public String performLogin(@RequestParam String email, 
                             @RequestParam String password, 
                             @RequestParam(required = false) String redirectUrl,
                             HttpSession session, 
                             Model model) {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (u.getUsername().equals(email)) {
                session.setAttribute("userId", u.getId());
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    return "redirect:" + redirectUrl;
                }
                return "redirect:/dashboard?userId=" + u.getId();
            }
        }
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            model.addAttribute("redirectUrl", redirectUrl);
        }
        model.addAttribute("error", "Invalid credentials");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String redirectUrl, Model model) {
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            model.addAttribute("redirectUrl", redirectUrl);
        }
        return "register";
    }

    @PostMapping("/register")
    public String performRegister(@RequestParam String name, 
                                @RequestParam String email, 
                                @RequestParam String password,
                                @RequestParam(required = false) String redirectUrl,
                                HttpSession session,
                                Model model) {
        User newUser = new User(email, name, "Asia/Kolkata");
        newUser = userRepository.save(newUser);
        session.setAttribute("userId", newUser.getId());
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/dashboard?userId=" + newUser.getId();
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long userId, HttpSession session, Model model) {
        if (userId == null) {
            userId = (Long) session.getAttribute("userId");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (userId == null && auth != null && auth.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            
            if (email != null) {
                User user = userRepository.findByUsername(email).orElse(null);
                if (user == null) {
                    user = new User(email, name, "Asia/Kolkata");
                    user.setGoogleSub(auth.getName());
                    user = userRepository.save(user);
                } else {
                    if (!auth.getName().equals(user.getGoogleSub())) {
                        user.setGoogleSub(auth.getName());
                        user = userRepository.save(user);
                    }
                }
                userId = user.getId();
                session.setAttribute("userId", userId);
            }
        }

        if (userId == null) {
            return "redirect:/login";
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
             return "redirect:/";
        }

        List<EventType> eventTypes = eventTypeRepository.findByUserId(userId);
        List<Booking> hostedBookings = bookingRepository.findAllByMentorId(userId);
        List<Booking> invitedBookings = bookingRepository.findAllByUserId(userId);
        
        Map<Long, String> userNames = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        hostedBookings.forEach(b -> { userIds.add(b.getUserId()); userIds.add(b.getMentorId()); });
        invitedBookings.forEach(b -> { userIds.add(b.getUserId()); userIds.add(b.getMentorId()); });
        
        for (Long id : userIds) {
            userRepository.findById(id).ifPresent(u -> userNames.put(id, u.getName()));
        }
        
        List<MeetingPoll> meetingPolls = meetingPollRepository.findAllByHostId(userId);
        
        // For Connected status in header and calendar prompt
        OAuth2User oauthUser = null;
        if (auth != null && auth.getPrincipal() instanceof OAuth2User) {
            oauthUser = (OAuth2User) auth.getPrincipal();
        }
        model.addAttribute("googleConnected", oauthUser != null || (user.getGoogleSub() != null));

        
        model.addAttribute("user", user);
        model.addAttribute("eventTypes", eventTypes);
        model.addAttribute("hostedBookings", hostedBookings);
        model.addAttribute("invitedBookings", invitedBookings);
        model.addAttribute("userNames", userNames);
        model.addAttribute("meetingPolls", meetingPolls);
        
        return "dashboard";
    }

    @PostMapping("/profile/update-link")
    public String updateMeetingLink(@RequestParam String googleMeetLink, @RequestParam Long userId, HttpSession session) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setGoogleMeetLink(googleMeetLink);
            userRepository.save(u);
        });
        return "redirect:/dashboard?userId=" + userId;
    }

    @PostMapping("/dashboard/dismiss-calendar-prompt")
    public String dismissCalendarPrompt(@RequestParam Long userId, HttpSession session) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setCalendarPromptDone(true);
            userRepository.save(u);
        });
        return "redirect:/dashboard?userId=" + userId;
    }

    @GetMapping("/settings/profile")
    public String profileSettings(@RequestParam(required = false) Long userId, HttpSession session, Model model) {
        if (userId == null) {
            userId = (Long) session.getAttribute("userId");
        }
        if (userId == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            
            // For Connected status in header
            OAuth2User oauthUser = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof OAuth2User) {
                oauthUser = (OAuth2User) auth.getPrincipal();
            }
            model.addAttribute("googleConnected", oauthUser != null || (user.getGoogleSub() != null));
            return "profile";
        }
        return "redirect:/login";
    }

    @PostMapping("/settings/profile/update")
    public String updateProfile(@RequestParam Long userId,
                                @RequestParam String name,
                                @RequestParam String welcomeMessage,
                                @RequestParam String language,
                                @RequestParam String dateFormat,
                                @RequestParam String timeFormat,
                                @RequestParam String country,
                                @RequestParam String time_zone,
                                @RequestParam(required = false) String googleMeetLink,
                                @RequestParam(required = false) MultipartFile profilePic,
                                HttpSession session) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setName(name);
            u.setWelcomeMessage(welcomeMessage);
            u.setLanguage(language);
            u.setDateFormat(dateFormat);
            u.setTimeFormat(timeFormat);
            u.setCountry(country);
            u.setTime_zone(time_zone);
            u.setGoogleMeetLink(googleMeetLink);

            
            if (profilePic != null && !profilePic.isEmpty()) {
                try {
                    String uploadDir = System.getProperty("user.dir") + "/uploads/profile-pics/";
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    String fileName = userId + "_" + System.currentTimeMillis() + "_" + profilePic.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(profilePic.getInputStream(), filePath);
                    
                    u.setProfilePicture("/uploads/profile-pics/" + fileName);
                } catch (IOException e) {
                    System.err.println("Failed to upload profile picture: " + e.getMessage());
                }
            }
            
            userRepository.save(u);
        });
        return "redirect:/settings/profile?userId=" + userId;
    }


    @GetMapping("/{username}")
    public String publicProfile(@PathVariable String username, 
                                @RequestParam(required = false) Long eventTypeId, 
                                @RequestParam(required = false) String date,
                                javax.servlet.http.HttpServletRequest request,
                                HttpSession session,
                                Model model) {
        Long loggedInUserId = (Long) session.getAttribute("userId");

        List<User> users = userRepository.findAll();
        User host = null;
        for (User u : users) {
             if (u.getUsername().equals(username)) {
                 host = u;
                 break;
             }
        }
        if (host == null) {
             return "redirect:/";
        }

        model.addAttribute("host", host);
        
        if (eventTypeId != null) {
            EventType et = eventTypeRepository.findById(eventTypeId).orElse(null);
            model.addAttribute("eventType", et);
            
            if (et != null) {
                boolean isOwner = (loggedInUserId != null && loggedInUserId.equals(et.getUserId()));
                model.addAttribute("isOwner", isOwner);

                if (isOwner) {
                    List<Booking> eventBookings = bookingRepository.findAll();
                    List<Booking> filteredBookings = new ArrayList<>();
                    Map<Long, String> bookingInviteeNames = new HashMap<>();

                    for (Booking b : eventBookings) {
                        if (et.getId().equals(b.getEventTypeId())) {
                            filteredBookings.add(b);
                            userRepository.findById(b.getUserId()).ifPresent(u -> bookingInviteeNames.put(b.getUserId(), u.getName()));
                        }
                    }
                    model.addAttribute("eventBookings", filteredBookings);
                    model.addAttribute("bookingInviteeNames", bookingInviteeNames);
                }

                if (!isOwner && et.isBooked()) {
                    model.addAttribute("error", et.getType().equals("GROUP") ? "This group event is already full." : "This invitation has already been used by someone else.");
                }

                // Generate weekday available dates for guests
                if (!isOwner && !et.isBooked()) {
                    try {
                        java.time.LocalDate start = java.time.LocalDate.parse(et.getStartDate());
                        java.time.LocalDate end = (et.getEndDate() != null && !et.getEndDate().isEmpty())
                            ? java.time.LocalDate.parse(et.getEndDate())
                            : start; // single day if no end date
                        java.time.LocalDate today = java.time.LocalDate.now();
                        
                        // Don't show past dates
                        if (start.isBefore(today)) start = today;
                        
                        // Each slot: [rawDate yyyy-MM-dd, displayLabel e.g. "Mon, Mar 9"]
                        List<String[]> availableSlots = new ArrayList<>();
                        java.time.LocalDate current = start;
                        java.time.format.DateTimeFormatter labelFmt = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.ENGLISH);
                        while (!current.isAfter(end) && availableSlots.size() < 60) {
                            java.time.DayOfWeek dow = current.getDayOfWeek();
                            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                                availableSlots.add(new String[]{ current.toString(), current.format(labelFmt) });
                            }
                            current = current.plusDays(1);
                        }
                        model.addAttribute("availableSlots", availableSlots);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }


            }
        } else {
            List<Opening> openings = (List<Opening>) openingRepository.findAllByOwner(host.getId());
            model.addAttribute("openings", openings);
            
            // Add all event types for this host so they can choose one
            List<EventType> eventTypes = eventTypeRepository.findByUserId(host.getId());
            model.addAttribute("eventTypes", eventTypes);
        }

        return "public-profile";
    }
    
    @GetMapping("/{username}/book")
    public String bookingForm(@PathVariable String username, 
                              @RequestParam String datetime, 
                              @RequestParam(required = false) Long eventTypeId, 
                              javax.servlet.http.HttpServletRequest request,
                              HttpSession session, 
                              Model model) {
        User host = userRepository.findByUsername(username).orElse(null);
        if (host == null) {
            return "redirect:/";
        }

        if (eventTypeId != null) {
            EventType et = eventTypeRepository.findById(eventTypeId).orElse(null);
            if (et != null && et.isBooked()) {
                 model.addAttribute("error", et.getType().equals("GROUP") ? "This group event is already full." : "This invitation has already been used by someone else.");
                 model.addAttribute("host", host);
                 return "public-profile";
            }
            model.addAttribute("eventType", et);
        }

        model.addAttribute("host", host);
        model.addAttribute("datetime", datetime);
        model.addAttribute("eventTypeId", eventTypeId);
        return "booking-form";
    }

    @PostMapping("/book-event")
    public String bookEvent(@RequestParam Long hostId, 
                          @RequestParam String inviteeName, 
                          @RequestParam String inviteeEmail,
                          @RequestParam(required = false) String message, 
                          @RequestParam String datetime, 
                          @RequestParam(required = false) Long eventTypeId,
                          HttpSession session,
                          Model model) {
        
        Long loggedInUserId = (Long) session.getAttribute("userId");
        List<Booking> existingBookings = bookingRepository.findAllByMentorId(hostId);
        int duration = 30;
        EventType etOpt = null;
        if (eventTypeId != null) {
            etOpt = eventTypeRepository.findById(eventTypeId).orElse(null);
            if (etOpt != null) {
                if (etOpt.isBooked() && etOpt.getType().equals("GROUP")) {
                     User host = userRepository.findById(hostId).orElse(null);
                     if (host != null) {
                         model.addAttribute("host", host);
                         model.addAttribute("error", "This group event is already full.");
                         model.addAttribute("datetime", datetime);
                         model.addAttribute("eventTypeId", eventTypeId);
                         model.addAttribute("eventType", etOpt);
                         return "booking-form";
                     }
                     return "redirect:/";
                }
                duration = etOpt.getDurationMinutes();
            }
        }
        
        User invitee = (loggedInUserId != null) ? userRepository.findById(loggedInUserId).orElse(null) : null;
        if (invitee == null) {
            invitee = userRepository.findByUsername(inviteeEmail).orElse(null);
        }
        if (invitee == null) {
             invitee = new User(inviteeEmail, inviteeName, "Asia/Kolkata");
             invitee = userRepository.save(invitee);
        }

        String location = (etOpt != null) ? etOpt.getLocation() : null;
        String locationDetails = (etOpt != null) ? etOpt.getLocationDetails() : null;
        Booking booking = new Booking(hostId, invitee.getId(), eventTypeId, message, datetime, location, locationDetails);
        bookingRepository.save(booking);
        
        if (etOpt != null) {
             if (etOpt.getType().equals("GROUP")) {
                 etOpt.setCurrentParticipants(etOpt.getCurrentParticipants() + 1);
                 if (etOpt.getCurrentParticipants() >= etOpt.getMaxParticipants()) {
                     etOpt.setBooked(true);
                 }
                 eventTypeRepository.save(etOpt);
             }
        }

        userRepository.findById(hostId).ifPresent(host -> {
            emailService.sendBookingRequestNotification(host.getUsername(), inviteeName, datetime);
        });

        model.addAttribute("booking", booking);
        userRepository.findById(hostId).ifPresent(host -> model.addAttribute("host", host));
        model.addAttribute("inviteeName", inviteeName);
        
        return "success";
    }

    @PostMapping("/event-types")
    public String createEventType(@RequestParam String title, 
                                  @RequestParam String description, 
                                  @RequestParam String startDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime,
                                  @RequestParam String eventTypeCategory,
                                  @RequestParam(required = false, defaultValue = "1") int maxParticipants,
                                  @RequestParam String location,
                                  @RequestParam(required = false) String locationDetails,
                                  @RequestParam Long userId) {
        
        int finalDuration = 30;
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
            try {
                LocalTime startT = LocalTime.parse(startTime);
                LocalTime endT = LocalTime.parse(endTime);
                finalDuration = (int) Duration.between(startT, endT).toMinutes();
                if (finalDuration < 0) finalDuration += 1440;
            } catch (Exception e) {}
        }
        String finalEndDate = (endDate != null && !endDate.isEmpty()) ? endDate : startDate;
        String eventTypeCategoryValue = (eventTypeCategory != null) ? eventTypeCategory : "ONE_ON_ONE";
        int finalMaxParticipants = (eventTypeCategoryValue.equals("GROUP")) ? maxParticipants : 1;

        String finalLocationDetails = locationDetails;
        if ("Google Meet".equals(location)) {
            Optional<User> hostOpt = userRepository.findById(userId);
            if (hostOpt.isPresent() && hostOpt.get().getGoogleMeetLink() != null && !hostOpt.get().getGoogleMeetLink().isEmpty()) {
                finalLocationDetails = hostOpt.get().getGoogleMeetLink();
            } else {
                String chars = "abcdefghijklmnopqrstuvwxyz";
                java.util.Random rnd = new java.util.Random();
                StringBuilder sb = new StringBuilder("https://meet.google.com/");
                for (int i = 0; i < 3; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
                sb.append("-");
                for (int i = 0; i < 4; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
                sb.append("-");
                for (int i = 0; i < 3; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
                finalLocationDetails = sb.toString();
            }
        }

        EventType newEventType = new EventType(title, description, finalDuration, userId, "#0069ff", startDate, finalEndDate, startTime, endTime, eventTypeCategoryValue, finalMaxParticipants, location, finalLocationDetails);
        eventTypeRepository.save(newEventType);
        return "redirect:/dashboard?userId=" + userId;
    }

    @PostMapping("/bookings/respond")
    public String respondToBooking(@RequestParam Long bookingId, @RequestParam String action, @RequestParam Long userId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking b = bookingOpt.get();
            if (b.getMentorId().equals(userId)) {
                String previousStatus = b.getStatus();
                if ("ACCEPT".equals(action)) {
                    b.setStatus("ACCEPTED");
                    if (b.getEventTypeId() != null) {
                        eventTypeRepository.findById(b.getEventTypeId()).ifPresent(et -> {
                            if (!"GROUP".equals(et.getType())) {
                                et.setBooked(true);
                                eventTypeRepository.save(et);
                            }
                        });
                    }
                } else if ("DECLINE".equals(action)) {
                    b.setStatus("DECLINED");
                }
                bookingRepository.save(b);

                if (!b.getStatus().equals(previousStatus)) {
                    Optional<User> hostOpt = userRepository.findById(b.getMentorId());
                    Optional<User> guestOpt = userRepository.findById(b.getUserId());
                    
                    if (hostOpt.isPresent() && guestOpt.isPresent()) {
                        User hostUser = hostOpt.get();
                        User guestUser = guestOpt.get();
                        emailService.sendBookingStatusNotification(guestUser.getUsername(), hostUser.getName(), b.getDate_time(), b.getStatus());
                        
                        if ("ACCEPTED".equals(b.getStatus())) {
                            int duration = 30;
                            String eventTitle = "Meeting with " + guestUser.getName();
                            String location = b.getLocation();
                            String locationDetails = b.getLocationDetails();
                            String eventEndDate = null; // null = single event

                            if (b.getEventTypeId() != null) {
                                Optional<EventType> etOpt2 = eventTypeRepository.findById(b.getEventTypeId());
                                if (etOpt2.isPresent()) {
                                    EventType et = etOpt2.get();
                                    duration = et.getDurationMinutes();
                                    eventTitle = et.getTitle() + " with " + guestUser.getName();
                                    // Determine if recurring: endDate is set and different from startDate
                                    if (et.getEndDate() != null && !et.getEndDate().isEmpty()
                                            && !et.getEndDate().equals(et.getStartDate())) {
                                        eventEndDate = et.getEndDate();
                                    }
                                }
                            }

                            String finalLocation = location;
                            if (!"Google Meet".equals(location) && locationDetails != null && !locationDetails.isEmpty()) {
                                finalLocation = location + " (" + locationDetails + ")";
                            }

                            if (eventEndDate != null) {
                                // Recurring Mon–Fri event for the entire date range
                                googleCalendarService.createRecurringCalendarEvent(
                                    hostUser.getUsername(),
                                    guestUser.getUsername(),
                                    eventTitle,
                                    b.getMessage(),
                                    b.getDate_time(),
                                    duration,
                                    finalLocation,
                                    eventEndDate
                                );
                            } else {
                                // Single one-time event
                                googleCalendarService.createCalendarEvent(
                                    hostUser.getUsername(),
                                    guestUser.getUsername(),
                                    eventTitle,
                                    b.getMessage(),
                                    b.getDate_time(),
                                    duration,
                                    finalLocation
                                );
                            }

                        }
                    }
                }
            }
        }
        return "redirect:/dashboard?userId=" + userId;
    }

    @PostMapping("/event-types/delete")
    public String deleteEventType(@RequestParam Long eventTypeId, @RequestParam Long userId) {
        Optional<EventType> etOpt = eventTypeRepository.findById(eventTypeId);
        if (etOpt.isPresent() && etOpt.get().getUserId().equals(userId)) {
            eventTypeRepository.delete(etOpt.get());
        }
        return "redirect:/dashboard?userId=" + userId;
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}