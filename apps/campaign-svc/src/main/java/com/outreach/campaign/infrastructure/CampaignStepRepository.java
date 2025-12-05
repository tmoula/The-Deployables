package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.CampaignStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignStepRepository extends JpaRepository<CampaignStepEntity, Integer> {
    List<CampaignStepEntity> findByCampaignId(Integer campaignId);
    List<CampaignStepEntity> findByCampaignIdOrderByStepOrderAsc(Integer campaignId);
}

