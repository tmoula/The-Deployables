package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, Integer> {
    List<CampaignEntity> findByUserId(Integer userId);
    List<CampaignEntity> findByStatus(String status);
}




