// The data model (record) representing a Lead (company, domain, email, etc.).

package com.outreach.lead.domain;
import java.util.List;

public record SellerProfile(
        String company,
        String industry,
        Integer companySize,
        List<String> valuePropositionKeywords,
        List<String> regions
) {}
