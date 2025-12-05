package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.EmailEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEventEntity, Integer> {
    List<EmailEventEntity> findBySentEmailId(Integer sentEmailId);
    List<EmailEventEntity> findByEventType(String eventType);
}

