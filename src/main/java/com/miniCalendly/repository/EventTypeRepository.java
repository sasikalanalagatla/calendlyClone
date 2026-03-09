package com.miniCalendly.repository;

import com.miniCalendly.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    List<EventType> findByUserId(Long userId);
}
