package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.SenderCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SenderCompanyRepository extends JpaRepository<SenderCompanyEntity, Integer> {
    List<SenderCompanyEntity> findByUserId(Integer userId);
    Optional<SenderCompanyEntity> findByUserIdAndId(Integer userId, Integer id);
}

