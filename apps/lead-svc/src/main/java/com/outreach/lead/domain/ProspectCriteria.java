package com.outreach.lead.domain;
import java.util.List;

public record ProspectCriteria(
        // all optional – user may fill only a few
        String industry,               // desired prospect industry (e.g., "Travel Tech")
        Integer minSize,               // min employees
        Integer maxSize,               // max employees
        List<String> regions,          // where the prospect operates
        List<String> requiredStack,    // must-have tools (e.g., ["Salesforce"])
        List<String> desiredKeywords   // nice-to-have topics (e.g., ["booking","personalization"])
) {}
