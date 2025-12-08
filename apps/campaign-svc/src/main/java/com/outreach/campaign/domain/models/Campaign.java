package com.outreach.campaign.domain.models;

import java.time.LocalDateTime;
import java.util.List;

public record Campaign(
        String id,
        String name,
        String description,
        CampaignStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<com.outreach.campaign.domain.models.Lead> leads,
        Integer totalLeads,
        Integer sentCount,
        Integer openedCount,
        Integer repliedCount,
        String csvFilename,
        List<String> csvColumns,
        String emailSubject,
        String emailBody
) {
    public enum CampaignStatus {
        DRAFT, RUNNING, PAUSED, COMPLETED
    }
}

