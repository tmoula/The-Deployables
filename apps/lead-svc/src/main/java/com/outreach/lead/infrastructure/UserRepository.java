package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserSummary, Integer> {
    Optional<UserSummary> findByEmail(String email);
}


