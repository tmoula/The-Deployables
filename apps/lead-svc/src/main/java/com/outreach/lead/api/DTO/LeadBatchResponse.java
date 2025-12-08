package com.outreach.lead.api.DTO;

public record LeadBatchResponse(
    Integer batchId,
    String status,
    String message
) {}


