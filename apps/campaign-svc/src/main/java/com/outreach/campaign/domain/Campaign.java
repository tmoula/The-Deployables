package com.outreach.campaign.domain;

import java.time.LocalDateTime;
import java.util.List;

public record Campaign(
        String id,
        String name,
        String description,
        CampaignStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Lead> leads,
        Integer totalLeads,
        Integer sentCount,
        Integer openedCount,
        Integer repliedCount
) {
    public enum CampaignStatus {
        DRAFT, RUNNING, PAUSED, COMPLETED
    }
}

