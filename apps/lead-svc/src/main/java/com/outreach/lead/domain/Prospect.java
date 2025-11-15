package com.outreach.lead.domain;
import java.util.List;

public record Prospect(
        String id,
        String company,
        String firstName,
        String lastName,
        String position,
        String email,
        String domain,
        String industry,
        Integer size,
        List<String> regions,
        List<String> stack,
        List<String> keywords,
        String personalizationHook
) {}
