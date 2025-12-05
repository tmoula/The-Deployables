package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.CampaignLeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignLeadRepository extends JpaRepository<CampaignLeadEntity, Integer> {
    List<CampaignLeadEntity> findByCampaignId(Integer campaignId);
    List<CampaignLeadEntity> findByLeadId(Integer leadId);
    List<CampaignLeadEntity> findByCampaignIdAndStatus(Integer campaignId, String status);
}

