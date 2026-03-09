package com.miniCalendly.repository;

import com.miniCalendly.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findAllByPollOptionId(Long pollOptionId);
}
