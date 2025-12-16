package com.outreach.lead.api.DTO;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DTOAndRecordTest {

    @Test
    void testLeadBatchResponse() {
        LeadBatchResponse response = new LeadBatchResponse(123, "pending", "Test message");
        assertEquals(123, response.batchId());
        assertEquals("pending", response.status());
        assertEquals("Test message", response.message());
    }

    @Test
    void testLeadBatchStatusResponse() {
        LeadBatchStatusResponse response = new LeadBatchStatusResponse(
            123, 100, 1, "api", "ready", 5, 10, null
        );
        assertEquals(123, response.id());
        assertEquals(100, response.userId());
        assertEquals(1, response.icpId());
        assertEquals("api", response.source());
        assertEquals("ready", response.status());
        assertEquals(5, response.requestedLeadCount());
        assertEquals(10, response.totalLeads());
        assertNull(response.errorMessage());
    }

    @Test
    void testLeadRequest() {
        LeadRequest request = new LeadRequest(
            "1", "Example Inc", "example.com", "CEO",
            "John Doe", "john@example.com", 100, "US", List.of("Java", "AWS")
        );
        assertEquals("1", request.id());
        assertEquals("Example Inc", request.company());
        assertEquals("example.com", request.domain());
        assertEquals("CEO", request.role());
        assertEquals("John Doe", request.name());
        assertEquals("john@example.com", request.email());
        assertEquals(100, request.size());
        assertEquals("US", request.region());
        assertEquals(List.of("Java", "AWS"), request.stack());
    }

    @Test
    void testProspect() {
        Prospect prospect = new Prospect(
            "1", "Example Inc", "John", "Doe", "CEO",
            "john@example.com", "example.com", "Tech", 100,
            List.of("US"), List.of("Java"), List.of("AI"), "Hook"
        );
        assertEquals("1", prospect.id());
        assertEquals("Example Inc", prospect.company());
        assertEquals("John", prospect.firstName());
        assertEquals("Doe", prospect.lastName());
        assertEquals("CEO", prospect.position());
        assertEquals("john@example.com", prospect.email());
        assertEquals("example.com", prospect.domain());
        assertEquals("Tech", prospect.industry());
        assertEquals(100, prospect.size());
        assertEquals(List.of("US"), prospect.regions());
        assertEquals(List.of("Java"), prospect.stack());
        assertEquals(List.of("AI"), prospect.keywords());
        assertEquals("Hook", prospect.personalizationHook());
    }

    @Test
    void testProspectCriteria() {
        ProspectCriteria criteria = new ProspectCriteria(
            "Example", "example.com", "Tech", 50, 500,
            2015, 2020,
            "Series A", "$1M-$10M", ">10% YoY",
            List.of("Java"), "Cloud Infrastructure",
            List.of("CEO"), "C-level",
            "North America", "US", true, List.of("US"),
            "Hiring in engineering", "Recently adopted Kubernetes",
            List.of("AI"), List.of("Java"), List.of("AI")
        );
        assertEquals("Example", criteria.companyName());
        assertEquals("example.com", criteria.domain());
        assertEquals("Tech", criteria.industry());
        assertEquals(50, criteria.minSize());
        assertEquals(500, criteria.maxSize());
        assertEquals(2015, criteria.minFoundedYear());
        assertEquals(2020, criteria.maxFoundedYear());
        assertEquals("Series A", criteria.fundingStage());
        assertEquals("$1M-$10M", criteria.annualRevenueRange());
        assertEquals(">10% YoY", criteria.growthRate());
        assertEquals(List.of("Java"), criteria.techUsed());
        assertEquals("Cloud Infrastructure", criteria.techCategory());
        assertEquals(List.of("CEO"), criteria.targetRoles());
        assertEquals("C-level", criteria.seniorityLevel());
        assertEquals("North America", criteria.headquartersRegion());
        assertEquals("US", criteria.hqCountry());
        assertTrue(criteria.remoteFriendly());
        assertEquals(List.of("US"), criteria.regions());
        assertEquals("Hiring in engineering", criteria.hiringTrends());
        assertEquals("Recently adopted Kubernetes", criteria.recentTechAdoption());
        assertEquals(List.of("AI"), criteria.keywordMentions());
        assertEquals(List.of("Java"), criteria.requiredStack());
        assertEquals(List.of("AI"), criteria.desiredKeywords());
    }

    @Test
    void testSellerProfile() {
        SellerProfile seller = new SellerProfile(
            "My Company", "Tech", 200, 2020, "US",
            List.of("AI"), "Enterprise", "Premium",
            List.of("Java"), "Outbound", List.of("US")
        );
        assertEquals("My Company", seller.companyName());
        assertEquals("Tech", seller.industry());
        assertEquals(200, seller.companySize());
        assertEquals(2020, seller.foundedYear());
        assertEquals("US", seller.headquartersRegion());
        assertEquals(List.of("AI"), seller.valuePropositionKeywords());
        assertEquals("Enterprise", seller.targetCustomerSegment());
        assertEquals("Premium", seller.priceTier());
        assertEquals(List.of("Java"), seller.techStack());
        assertEquals("Outbound", seller.salesModel());
        assertEquals(List.of("US"), seller.targetRegions());
    }
}
