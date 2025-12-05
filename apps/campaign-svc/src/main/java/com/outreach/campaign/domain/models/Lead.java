package com.outreach.campaign.domain.models;

public record Lead(
        String id,
        Double matchScore,
        String company,
        String firstName,
        String lastName,
        String position,
        String email,
        String domain,
        String industry,
        Integer companySize,
        String regions,
        String techStack,
        String keywords,
        String notes,
        String personalizationHook
) {}

