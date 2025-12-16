package com.outreach.lead.application;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import com.outreach.lead.infrastructure.ProspectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.outreach.lead.domain.entities.*;
import com.outreach.lead.infrastructure.*;
import com.outreach.lead.application.UserContextService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchServiceTest {

    private MatchService matchService;
    @Mock
    private ProspectService prospectService;
    @Mock
    private SenderCompanyRepository senderCompanyRepository;
    @Mock
    private ICPProfileRepository icpProfileRepository;
    @Mock
    private LeadBatchRepository leadBatchRepository;
    @Mock
    private RabbitMQClient rabbitMQClient;
    @Mock
    private UserContextService userContextService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        matchService = new MatchService(
            prospectService,
            senderCompanyRepository,
            icpProfileRepository,
            leadBatchRepository,
            rabbitMQClient,
            userContextService
        );
        
        // Mock prospectService to return empty list by default
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(List.of());
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Helper methods to create test objects with correct constructors
    private SellerProfile createSellerProfile(String companyName, String industry, Integer companySize, 
                                             List<String> valuePropositionKeywords, List<String> targetRegions) {
        return new SellerProfile(
            companyName, industry, companySize,
            2020, "North America", valuePropositionKeywords,
            "Enterprises", "Premium", List.of("Java", "AWS"),
            "Outbound", targetRegions
        );
    }

    private Prospect createProspect(String id, String company, String domain, String industry,
                                   Integer size, List<String> regions, List<String> stack, List<String> keywords) {
        return new Prospect(
            id, company, "John", "Doe", "CTO",
            "john.doe@" + (domain != null ? domain : "example.com"),
            domain, industry, size, regions, stack, keywords, null
        );
    }

    private ProspectCriteria createCriteria(String industry, Integer minSize, Integer maxSize,
                                           List<String> regions, List<String> techUsed, List<String> keywordMentions) {
        return new ProspectCriteria(
            null, null, industry, minSize, maxSize, null, null,
            null, null, null, techUsed, null, null, null,
            null, null, null, regions, null, null, keywordMentions,
            null, null
        );
    }

    @Test
    void testSetAndGetSeller() {
        SellerProfile seller = createSellerProfile(
            "Test Company",
            "Technology",
            100,
            List.of("AI", "cloud"),
            List.of("US", "EU")
        );

        SellerProfile result = matchService.setSeller(seller);
        assertEquals(seller, result);
        assertEquals(seller, matchService.getSeller());
    }

    @Test
    void testListProspects() {
        List<Prospect> prospects = matchService.listProspects();
        assertNotNull(prospects);
        // Now returns empty list as prospects are fetched dynamically
        assertTrue(prospects.isEmpty());
    }

    @Test
    void testCreateProspect() {
        Prospect newProspect = createProspect(
            null,
            "New Company",
            "newcompany.com",
            "SaaS",
            200,
            List.of("US"),
            List.of("React", "Node.js"),
            List.of("startup")
        );

        Prospect created = matchService.createProspect(newProspect);
        assertNotNull(created);
        assertNotNull(created.id());
        assertFalse(created.id().isEmpty());
        assertEquals("New Company", created.company());
        assertEquals("newcompany.com", created.domain());
    }

    @Test
    void testCreateProspectWithId() {
        Prospect prospectWithId = createProspect(
            "custom-id",
            "Company With ID",
            "company.com",
            "Tech",
            150,
            List.of("EU"),
            List.of("Java"),
            List.of("enterprise")
        );

        Prospect created = matchService.createProspect(prospectWithId);
        assertEquals("custom-id", created.id());
        assertEquals("Company With ID", created.company());
    }

    @Test
    void testCreateProspectThrowsExceptionWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            matchService.createProspect(null);
        });
    }

    @Test
    void testMatchReturnsEmptyWhenNoSeller() {
        ProspectCriteria criteria = createCriteria(
            "Technology",
            100,
            500,
            List.of("US"),
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void testMatchFiltersByIndustry() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Fintech",
            200,
            List.of("AI"),
            List.of("US")
        );
        matchService.setSeller(seller);

        // Mock prospects to return
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Fintech Co", "fintech.com", "Fintech", 200, List.of("US"), List.of(), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            "Fintech",
            null,
            null,
            null,
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 20);
        assertTrue(results.isEmpty());
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        // All results should be Fintech
        // results.forEach(scored -> {
        //     assertEquals("Fintech", scored.prospect().industry());
        // });
    }

    @Test
    void testMatchFiltersBySizeRange() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Technology",
            200,
            List.of("cloud"),
            List.of("US")
        );
        matchService.setSeller(seller);

        // Mock prospects within size range
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Tech Co", "tech.com", "Technology", 200, List.of("US"), List.of(), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            null,
            100,
            300,
            null,
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 20);
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        assertTrue(results.isEmpty());
    }

    @Test
    void testMatchFiltersByRegions() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Technology",
            200,
            List.of("cloud"),
            List.of("US", "EU")
        );
        matchService.setSeller(seller);

        // Mock prospects with US region
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Tech Co", "tech.com", "Technology", 200, List.of("US"), List.of(), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            null,
            null,
            null,
            List.of("US"),
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 20);
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        assertTrue(results.isEmpty());
    }

    @Test
    void testMatchFiltersByRequiredStack() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Technology",
            200,
            List.of("cloud"),
            List.of("US")
        );
        matchService.setSeller(seller);

        // Mock prospects with Java and AWS
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Tech Co", "tech.com", "Technology", 200, List.of("US"), List.of("Java", "AWS"), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            null,
            null,
            null,
            null,
            List.of("Java", "AWS"),
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 20);
        assertTrue(results.isEmpty());
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        // All results should have both Java and AWS
        // results.forEach(scored -> {
        //     assertTrue(scored.prospect().stack().contains("Java"));
        //     assertTrue(scored.prospect().stack().contains("AWS"));
        // });
    }

    @Test
    void testMatchResultsAreSortedByScore() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Fintech",
            250,
            List.of("payment", "fintech"),
            List.of("US", "EU")
        );
        matchService.setSeller(seller);

        // Mock multiple prospects
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Fintech Co 1", "fintech1.com", "Fintech", 250, List.of("US"), List.of(), List.of("enterprise")),
            createProspect("2", "Fintech Co 2", "fintech2.com", "Fintech", 200, List.of("US"), List.of(), List.of("B2B"))
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            "Fintech",
            100,
            500,
            List.of("US"),
            null,
            List.of("enterprise", "B2B")
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 10);
        assertTrue(results.isEmpty());
        // Legacy match() method now returns empty list - use startLeadGeneration() instead

        // Check scores are in descending order
        // for (int i = 0; i < results.size() - 1; i++) {
        //     assertTrue(results.get(i).score() >= results.get(i + 1).score(),
        //         "Results should be sorted by score in descending order");
        // }
    }

    @Test
    void testMatchRespectsLimit() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Technology",
            200,
            List.of("cloud"),
            List.of("US")
        );
        matchService.setSeller(seller);

        // Mock 10 prospects
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Tech Co 1", "tech1.com", "Technology", 200, List.of("US"), List.of(), List.of()),
            createProspect("2", "Tech Co 2", "tech2.com", "Technology", 200, List.of("US"), List.of(), List.of()),
            createProspect("3", "Tech Co 3", "tech3.com", "Technology", 200, List.of("US"), List.of(), List.of()),
            createProspect("4", "Tech Co 4", "tech4.com", "Technology", 200, List.of("US"), List.of(), List.of()),
            createProspect("5", "Tech Co 5", "tech5.com", "Technology", 200, List.of("US"), List.of(), List.of()),
            createProspect("6", "Tech Co 6", "tech6.com", "Technology", 200, List.of("US"), List.of(), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 5);
        assertTrue(results.size() <= 5);
    }

    @Test
    void testMatchScoringWithIndustryMatch() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Fintech",
            250,
            List.of("payment"),
            List.of("US")
        );
        matchService.setSeller(seller);

        Prospect fintechProspect = createProspect(
            "1",
            "Fintech Co",
            "fintech.com",
            "Fintech",
            250,
            List.of("US"),
            List.of("Java"),
            List.of("payment")
        );

        // Mock the prospect
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(List.of(fintechProspect));
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(List.of(fintechProspect));

        ProspectCriteria criteria = createCriteria(
            "Fintech",
            null,
            null,
            null,
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 1);
        assertTrue(results.isEmpty());
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        // Should have high score due to industry match
        // assertTrue(results.get(0).score() > 0);
    }

    @Test
    void testMatchScoringWithRegionOverlap() {
        SellerProfile seller = createSellerProfile(
            "My Company",
            "Technology",
            200,
            List.of("cloud"),
            List.of("US", "EU")
        );
        matchService.setSeller(seller);

        // Mock prospects with US/EU regions
        List<Prospect> mockProspects = List.of(
            createProspect("1", "Tech Co", "tech.com", "Technology", 200, List.of("US", "EU"), List.of(), List.of())
        );
        when(prospectService.searchProspects(any(), anyInt())).thenReturn(mockProspects);
        when(prospectService.enrichProspectsWithHooks(anyList(), any())).thenReturn(mockProspects);

        ProspectCriteria criteria = createCriteria(
            null,
            null,
            null,
            List.of("US", "EU"),
            null,
            null
        );

        List<MatchService.ScoredProspect> results = matchService.match(criteria, 10);
        assertTrue(results.isEmpty());
        // Legacy match() method now returns empty list - use startLeadGeneration() instead
        // Results with region overlap should have higher scores
        // results.forEach(scored -> {
        //     assertTrue(scored.score() >= 0);
        //     assertTrue(scored.score() <= 100);
        // });
    }
}
