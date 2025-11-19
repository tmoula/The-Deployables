package com.outreach.lead.domain;
import java.util.List;

public record ProspectCriteria(
        // General
        String companyName,
        String domain,
        String industry,
        Integer minSize,
        Integer maxSize,
        Integer minFoundedYear,
        Integer maxFoundedYear,
        
        // Financials
        String fundingStage,           // "Seed", "Series A", "Series B", "IPO"
        String annualRevenueRange,     // "$1M–$10M"
        String growthRate,             // ">10% YoY"
        
        // Technology
        List<String> techUsed,
        String techCategory,           // "Cloud Infrastructure", "CRM", "Payment Gateway"
        
        // Decision Makers
        List<String> targetRoles,      // "CTO, VP Engineering, Head of AI"
        String seniorityLevel,         // "C-level", "VP", "Director", "Manager"
        
        // Geography
        String headquartersRegion,     // "North America", "Europe", "MENA"
        String hqCountry,              // "US", "UK", "France"
        Boolean remoteFriendly,
        List<String> regions,
        
        // Behavioral / Intent Data
        String hiringTrends,            // "Hiring in engineering"
        String recentTechAdoption,     // "Recently adopted Kubernetes"
        List<String> keywordMentions,  // "AI, automation"
        
        // Legacy fields (for backward compatibility)
        List<String> requiredStack,
        List<String> desiredKeywords
) {}
