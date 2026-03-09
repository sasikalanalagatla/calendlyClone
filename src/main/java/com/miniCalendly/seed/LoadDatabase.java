package com.miniCalendly.seed;

import com.miniCalendly.model.Booking;
import com.miniCalendly.model.EventType;
import com.miniCalendly.model.Opening;
import com.miniCalendly.model.User;
import com.miniCalendly.repository.BookingRepository;
import com.miniCalendly.repository.EventTypeRepository;
import com.miniCalendly.repository.OpeningRepository;
import com.miniCalendly.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class LoadDatabase {
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);
    @Bean
    CommandLineRunner initUserTable(UserRepository repository) {
        return args -> {
            log.info("Preloading " + repository
                    .save(new User("bilbo", "Bilbo Baggins", "03:00")));
            log.info("Preloading " + repository
                    .save(new User("frodo", "Frodo Baggins", "02:00")));
        };
    }

    @Bean
    CommandLineRunner initEventTypeTable(EventTypeRepository repository, UserRepository userRepository) {
        return args -> {
            User user1 = userRepository.findById(1L).orElse(null);
            if (user1 != null) {
                repository.save(new EventType("15 Minute Meeting", "Quick sync.", 15, 1L, "#0069ff", "2026-03-08", "2026-03-15", "09:00", "09:15", "ONE_ON_ONE", 1, "Google Meet", "https://meet.google.com/abc-defg-hij"));
                repository.save(new EventType("30 Minute Meeting", "Standard consultation.", 30, 1L, "#0069ff", "2026-03-09", "2026-03-16", "10:30", "11:00", "ONE_ON_ONE", 1, "Phone Call", "9876543210"));
                repository.save(new EventType("Group Workshop", "Deep dive discussion.", 60, 1L, "#0069ff", "2026-03-10", "2026-03-17", "14:00", "15:00", "GROUP", 30, "In-person Meeting", "Building Alpha, Room 101"));
            }
        };
    }

    @Bean
    CommandLineRunner initOpeningTable(OpeningRepository repository) {
        return args -> {
            repository.save(new Opening(1L, "Monday: 09:00-17:00"));
            repository.save(new Opening(1L, "Tuesday: 09:00-17:00"));
            repository.save(new Opening(1L, "Wednesday: 09:00-17:00"));
            repository.save(new Opening(1L, "Thursday: 09:00-17:00"));
            repository.save(new Opening(1L, "Friday: 09:00-17:00"));
        };
    }

    @Bean
    CommandLineRunner initBookingTable(BookingRepository repository) {
        return args -> {
            // repository.save(new Booking(1L, 2L, 1L, "Initial booking", "2026-03-10 10:00", "Google Meet", "https://meet.google.com/abc-defg-hij"));
        };
    }
}
