package com.outreach.lead.api.DTO;

public record LeadBatchStatusResponse(
    Integer id,
    Integer userId,
    Integer icpId,
    String source,
    String status,
    Integer requestedLeadCount,
    Integer totalLeads,
    String errorMessage
) {}

