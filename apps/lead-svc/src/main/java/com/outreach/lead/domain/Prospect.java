package com.outreach.lead.domain;
import java.util.List;

public record Prospect(
        String id,
        String company,
        String domain,
        String industry,
        Integer size,
        List<String> regions,
        List<String> stack,
        List<String> keywords
) {}
