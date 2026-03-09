package com.miniCalendly.repository;

import com.miniCalendly.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findAllByPollId(Long pollId);
}
