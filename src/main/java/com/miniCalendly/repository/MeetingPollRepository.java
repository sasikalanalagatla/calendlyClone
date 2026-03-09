package com.miniCalendly.repository;

import com.miniCalendly.model.MeetingPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingPollRepository extends JpaRepository<MeetingPoll, Long> {
    List<MeetingPoll> findAllByHostId(Long hostId);
}
