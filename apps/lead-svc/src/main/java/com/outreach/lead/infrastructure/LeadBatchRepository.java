package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.LeadBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadBatchRepository extends JpaRepository<LeadBatchEntity, Integer> {
    List<LeadBatchEntity> findByUserId(Integer userId);
    Optional<LeadBatchEntity> findByIdAndUserId(Integer id, Integer userId);
}

