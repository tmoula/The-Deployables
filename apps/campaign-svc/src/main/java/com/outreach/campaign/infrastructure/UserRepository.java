package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserSummary, Integer> {
    Optional<UserSummary> findByEmail(String email);
}


