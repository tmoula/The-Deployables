package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.MailboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailboxRepository extends JpaRepository<MailboxEntity, Integer> {
    List<MailboxEntity> findByUserId(Integer userId);
    Optional<MailboxEntity> findByUserIdAndId(Integer userId, Integer id);
    Optional<MailboxEntity> findByEmailAddress(String emailAddress);
}

