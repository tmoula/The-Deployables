package com.outreach.lead.domain;
import java.util.List;

public record SellerProfile(
        // Company Basics
        String companyName,
        String industry,
        Integer companySize,
        Integer foundedYear,
        String headquartersRegion,
        
        // Product / Offering
        List<String> valuePropositionKeywords,
        String targetCustomerSegment,  // "Enterprises", "SMBs", "B2B startups"
        String priceTier,              // "Premium", "Mid-market", "Freemium"
        
        // Technology Stack
        List<String> techStack,
        
        // Go-to-Market
        String salesModel,             // "Inside Sales", "Outbound", "Product-led Growth"
        List<String> targetRegions
) {}
