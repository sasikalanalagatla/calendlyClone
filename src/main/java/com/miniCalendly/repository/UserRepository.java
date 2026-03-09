package com.miniCalendly.repository;

import com.miniCalendly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    java.util.Optional<com.miniCalendly.model.User> findByUsername(String username);
}
