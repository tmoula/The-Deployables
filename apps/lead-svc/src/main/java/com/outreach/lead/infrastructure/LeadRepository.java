package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<LeadEntity, Integer> {
    List<LeadEntity> findByUserId(Integer userId);
    List<LeadEntity> findByBatchId(Integer batchId);
    List<LeadEntity> findByUserIdAndBatchId(Integer userId, Integer batchId);
}

