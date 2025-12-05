package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.LeadBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeadBatchRepository extends JpaRepository<LeadBatchEntity, Integer> {
    List<LeadBatchEntity> findByUserId(Integer userId);
    Optional<LeadBatchEntity> findByUserIdAndId(Integer userId, Integer id);
    List<LeadBatchEntity> findByUserIdAndStatus(Integer userId, String status);
}

