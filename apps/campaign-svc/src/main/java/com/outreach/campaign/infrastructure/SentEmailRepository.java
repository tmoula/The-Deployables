package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.SentEmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SentEmailRepository extends JpaRepository<SentEmailEntity, Integer> {
    List<SentEmailEntity> findByCampaignId(Integer campaignId);
    List<SentEmailEntity> findByLeadId(Integer leadId);
    List<SentEmailEntity> findByStatus(String status);
    List<SentEmailEntity> findByCampaignIdAndStatus(Integer campaignId, String status);
}

