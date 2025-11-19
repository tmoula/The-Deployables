package com.outreach.lead.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    void testProspectRecord() {
        Prospect prospect = new Prospect(
            "1",
            "Test Company",
            "John",
            "Doe",
            "CTO",
            "john.doe@test.com",
            "test.com",
            "Technology",
            100,
            List.of("US", "EU"),
            List.of("Java", "AWS"),
            List.of("enterprise"),
            null
        );

        assertEquals("1", prospect.id());
        assertEquals("Test Company", prospect.company());
        assertEquals("test.com", prospect.domain());
        assertEquals("Technology", prospect.industry());
        assertEquals(100, prospect.size());
        assertEquals(List.of("US", "EU"), prospect.regions());
        assertEquals(List.of("Java", "AWS"), prospect.stack());
        assertEquals(List.of("enterprise"), prospect.keywords());
    }

    @Test
    void testSellerProfileRecord() {
        SellerProfile seller = new SellerProfile(
            "My Company",
            "Technology",
            200,
            2020,
            "North America",
            List.of("AI", "cloud"),
            "Enterprises",
            "Premium",
            List.of("Java", "AWS"),
            "Outbound",
            List.of("US", "EU")
        );

        assertEquals("My Company", seller.companyName());
        assertEquals("Technology", seller.industry());
        assertEquals(200, seller.companySize());
        assertEquals(List.of("AI", "cloud"), seller.valuePropositionKeywords());
        assertEquals(List.of("US", "EU"), seller.targetRegions());
    }

    @Test
    void testProspectCriteriaRecord() {
        ProspectCriteria criteria = new ProspectCriteria(
            null,                     // companyName
            null,                     // domain
            "Fintech",                // industry
            100,                      // minSize
            500,                      // maxSize
            null,                     // minFoundedYear
            null,                     // maxFoundedYear
            null,                     // fundingStage
            null,                     // annualRevenueRange
            null,                     // growthRate
            List.of("Java", "AWS"),   // techUsed
            null,                     // techCategory
            null,                     // targetRoles
            null,                     // seniorityLevel
            null,                     // headquartersRegion
            null,                     // hqCountry
            null,                     // remoteFriendly
            List.of("US", "EU"),      // regions
            null,                     // hiringTrends
            null,                     // recentTechAdoption
            List.of("enterprise", "B2B"), // keywordMentions
            null,                     // requiredStack (legacy)
            null                      // desiredKeywords (legacy)
        );

        assertEquals("Fintech", criteria.industry());
        assertEquals(100, criteria.minSize());
        assertEquals(500, criteria.maxSize());
        assertEquals(List.of("US", "EU"), criteria.regions());
        assertEquals(List.of("Java", "AWS"), criteria.techUsed());
        assertEquals(List.of("enterprise", "B2B"), criteria.keywordMentions());
    }

    @Test
    void testProspectCriteriaWithNullValues() {
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null
        );

        assertNull(criteria.industry());
        assertNull(criteria.minSize());
        assertNull(criteria.maxSize());
        assertNull(criteria.regions());
        assertNull(criteria.techUsed());
        assertNull(criteria.keywordMentions());
    }

    @Test
    void testProspectWithNullOptionalFields() {
        Prospect prospect = new Prospect(
            "1",
            "Company",
            "John",
            "Doe",
            "CTO",
            "john.doe@company.com",
            "company.com",
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("1", prospect.id());
        assertEquals("Company", prospect.company());
        assertEquals("company.com", prospect.domain());
        assertNull(prospect.industry());
        assertNull(prospect.size());
        assertNull(prospect.regions());
        assertNull(prospect.stack());
        assertNull(prospect.keywords());
    }
}

