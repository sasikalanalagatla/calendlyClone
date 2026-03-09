package com.miniCalendly.controller;

import com.miniCalendly.model.*;
import com.miniCalendly.repository.*;
import com.miniCalendly.service.EmailService;
import com.miniCalendly.service.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/polls")
public class MeetingPollController {

    @Autowired
    private MeetingPollRepository pollRepository;
    @Autowired
    private PollOptionRepository optionRepository;
    @Autowired
    private PollVoteRepository voteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private GoogleCalendarService googleCalendarService;

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        userRepository.findById(userId).ifPresent(user -> model.addAttribute("user", user));
        return "create-poll";
    }

    @PostMapping("/create")
    public String createPoll(@RequestParam String title, 
                             @RequestParam String description,
                             @RequestParam("dateTime") List<String> dateTimes,
                             @RequestParam int duration,
                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        MeetingPoll poll = new MeetingPoll(title, description, userId);
        pollRepository.save(poll);

        for (String dt : dateTimes) {
            if (dt != null && !dt.isEmpty()) {
                String formattedDt = dt.replace("T", " ");
                PollOption option = new PollOption(poll.getId(), formattedDt, duration);
                optionRepository.save(option);
            }
        }

        return "redirect:/dashboard?userId=" + userId;
    }

    @GetMapping("/{id}/vote")
    public String showVotePage(@PathVariable Long id, Model model) {
        Optional<MeetingPoll> pollOpt = pollRepository.findById(id);
        if (!pollOpt.isPresent()) return "redirect:/";

        MeetingPoll poll = pollOpt.get();
        List<PollOption> options = optionRepository.findAllByPollId(poll.getId());
        User host = userRepository.findById(poll.getHostId()).orElse(null);

        model.addAttribute("poll", poll);
        model.addAttribute("options", options);
        model.addAttribute("host", host);
        return "poll-vote";
    }

    @PostMapping("/vote")
    public String submitVote(@RequestParam Long pollId,
                             @RequestParam String inviteeName,
                             @RequestParam String inviteeEmail,
                             @RequestParam(value = "optionIds", required = false) List<Long> selectedOptionIds) {
        
        List<PollOption> allOptions = optionRepository.findAllByPollId(pollId);
        Set<Long> selectedSet = selectedOptionIds != null ? new HashSet<>(selectedOptionIds) : new HashSet<>();

        for (PollOption opt : allOptions) {
            String status = selectedSet.contains(opt.getId()) ? "AVAILABLE" : "NOT_AVAILABLE";
            PollVote vote = new PollVote(opt.getId(), inviteeEmail, inviteeName, status);
            voteRepository.save(vote);
        }

        return "poll-thanks";
    }

    @GetMapping("/{id}/results")
    public String showResults(@PathVariable Long id, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<MeetingPoll> pollOpt = pollRepository.findById(id);
        if (!pollOpt.isPresent()) return "redirect:/dashboard";

        MeetingPoll poll = pollOpt.get();
        if (!poll.getHostId().equals(userId)) return "redirect:/dashboard";

        List<PollOption> options = optionRepository.findAllByPollId(poll.getId());
        Map<Long, List<PollVote>> votesByOption = new HashMap<>();
        Map<Long, Long> voteCounts = new HashMap<>();

        for (PollOption opt : options) {
            List<PollVote> votes = voteRepository.findAllByPollOptionId(opt.getId())
                    .stream()
                    .filter(v -> "AVAILABLE".equals(v.getStatus()))
                    .collect(Collectors.toList());
            votesByOption.put(opt.getId(), votes);
            voteCounts.put(opt.getId(), (long) votes.size());
        }

        model.addAttribute("poll", poll);
        model.addAttribute("options", options);
        model.addAttribute("votesByOption", votesByOption);
        model.addAttribute("voteCounts", voteCounts);
        return "poll-details";
    }

    @PostMapping("/{id}/confirm")
    public String confirmSlot(@PathVariable Long id, @RequestParam Long optionId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        MeetingPoll poll = pollRepository.findById(id).orElse(null);
        PollOption option = optionRepository.findById(optionId).orElse(null);

        if (poll != null && option != null && poll.getHostId().equals(userId)) {
            poll.setStatus("CLOSED");
            pollRepository.save(poll);

            User host = userRepository.findById(poll.getHostId()).orElse(null);
            
            // Send confirmation to everyone who voted AVAILABLE for this slot
            List<PollVote> winningVotes = voteRepository.findAllByPollOptionId(option.getId())
                    .stream()
                    .filter(v -> "AVAILABLE".equals(v.getStatus()))
                    .collect(Collectors.toList());

            for (PollVote v : winningVotes) {
                // Create Calendar Event for Host with Guest
                googleCalendarService.createCalendarEvent(
                    host.getUsername(),
                    v.getInviteeEmail(),
                    "Meeting: " + poll.getTitle(),
                    poll.getDescription(),
                    option.getDateTime(),
                    option.getDurationMinutes(),
                    "Google Meet"
                );

                // Send email notification
                emailService.sendBookingStatusNotification(
                    v.getInviteeEmail(),
                    host.getName(),
                    option.getDateTime(),
                    "ACCEPTED"
                );
            }
        }

        return "redirect:/dashboard?userId=" + userId;
    }
}
