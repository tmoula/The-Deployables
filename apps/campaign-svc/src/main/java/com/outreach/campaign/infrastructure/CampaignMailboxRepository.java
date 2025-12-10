package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.CampaignMailboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignMailboxRepository extends JpaRepository<CampaignMailboxEntity, Integer> {
    List<CampaignMailboxEntity> findByCampaignId(Integer campaignId);
    void deleteByCampaignId(Integer campaignId);
    List<CampaignMailboxEntity> findByCampaignIdOrderByPriorityDesc(Integer campaignId);
}

