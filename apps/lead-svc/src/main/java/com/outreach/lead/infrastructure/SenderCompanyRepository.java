package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.SenderCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SenderCompanyRepository extends JpaRepository<SenderCompanyEntity, Integer> {
    Optional<SenderCompanyEntity> findByUserId(Integer userId);
}


