package com.outreach.campaign.infrastructure;

import com.outreach.campaign.domain.entities.ICPProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ICPProfileRepository extends JpaRepository<ICPProfileEntity, Integer> {
    List<ICPProfileEntity> findByUserId(Integer userId);
    Optional<ICPProfileEntity> findByUserIdAndId(Integer userId, Integer id);
}

