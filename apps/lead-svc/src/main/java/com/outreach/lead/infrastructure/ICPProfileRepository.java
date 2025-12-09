package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.ICPProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICPProfileRepository extends JpaRepository<ICPProfileEntity, Integer> {
    List<ICPProfileEntity> findByUserId(Integer userId);
    Optional<ICPProfileEntity> findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
        Integer userId, String targetIndustry, Integer companySizeMin, Integer companySizeMax
    );
}




