package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectServiceTest {

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private RabbitMQClient rabbitMQClient;

    @InjectMocks
    private ProspectService prospectService;

    private SellerProfile testSeller;
    private Prospect testProspect;

    @BeforeEach
    void setUp() {
        testSeller = new SellerProfile(
            "Test Seller Corp", "Technology", 200, 2020, "US",
            List.of("AI", "ML"), "Enterprise", "Premium", List.of("Python", "TensorFlow"),
            "Outbound", List.of("US", "EU")
        );

        testProspect = new Prospect(
            "1", "Test Company", "John", "Doe", "CTO",
            "john@test.com", "test.com", "Technology", 100,
            List.of("US"), List.of("Java"), List.of("cloud"), null
        );
    }

    @Test
    void testEnrichProspectsWithHooks_WithValidProspects_ReturnsEnrichedProspects() {
        // Given
        List<Prospect> prospects = List.of(testProspect);
        String hook = "I noticed Test Company is using Java for cloud infrastructure";
        
        when(aiServiceClient.generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class)))
            .thenReturn(hook);
        
        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(prospects, testSeller);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(hook, result.get(0).personalizationHook());
        verify(aiServiceClient, times(1)).generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class));
    }

    @Test
    void testEnrichProspectsWithHooks_WithException_ReturnsProspectsWithFallbackHook() {
        // Given
        List<Prospect> prospects = List.of(testProspect);
        
        when(aiServiceClient.generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class)))
            .thenThrow(new RuntimeException("AI service error"));
        
        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(prospects, testSeller);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).personalizationHook());
        assertTrue(result.get(0).personalizationHook().contains("Test Company"));
    }

    @Test
    void testEnrichProspectsWithHooks_WithMultipleProspects_EnrichesAll() {
        // Given
        Prospect prospect2 = new Prospect(
            "2", "Another Company", "Jane", "Smith", "VP Engineering",
            "jane@another.com", "another.com", "Technology", 150,
            List.of("US"), List.of("Python"), List.of("AI"), null
        );
        List<Prospect> prospects = List.of(testProspect, prospect2);
        
        when(aiServiceClient.generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class)))
            .thenReturn("Generated hook");
        
        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(prospects, testSeller);
        
        // Then
        assertEquals(2, result.size());
        verify(aiServiceClient, times(2)).generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class));
    }

    @Test
    void testSearchProspects_WithValidCriteria_ReturnsProspects() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", 50, 200, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        List<String> domains = List.of("example.com", "test.com");
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(domains);
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(aiServiceClient, times(1)).generateMatchingCompanies(any(ProspectCriteria.class), eq(5));
    }

    @Test
    void testSearchProspects_WithLimitOver5_LimitsTo5() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        List<String> domains = List.of("example.com");
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(domains);
        
        // When
        prospectService.searchProspects(criteria, 10);
        
        // Then - verify that limit was capped at 5
        verify(aiServiceClient, times(1)).generateMatchingCompanies(any(ProspectCriteria.class), eq(5));
    }

    @Test
    void testSearchProspects_WithNullDomains_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(null);
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProspects_WithEmptyDomains_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProspects_WithException_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenThrow(new RuntimeException("AI service error"));
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProspects_WithTargetRoles_UsesFirstRoleAsPosition() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null,
            null, null, null, null, null,
            List.of("CTO", "VP Engineering"), "Senior",
            null, null, null, null,
            null, null, List.of("enterprise"),
            null, null
        );
        
        List<String> domains = List.of("example.com");
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(domains);
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertFalse(result.isEmpty());
        assertEquals("CTO", result.get(0).position());
    }

    @Test
    void testSearchProspects_WithSeniorityLevel_GeneratesAppropriatePosition() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null,
            null, null, null, null, null,
            null, "Executive",
            null, null, null, null,
            null, null, null,
            null, null
        );
        
            List<String> domains = List.of("example.com");
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(domains);
        
        // When
        List<Prospect> prospects = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertFalse(prospects.isEmpty());
        assertNotNull(prospects.get(0).position());
        // Position should be generated based on seniority level
        assertTrue(prospects.get(0).position().equals("CEO") || 
                   prospects.get(0).position().equals("Decision Maker"));
    }

    @Test
    void testSearchProspects_WithEmptyDomain_SkipsProspect() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        List<String> domains = List.of("example.com", "", "test.com");
        when(aiServiceClient.generateMatchingCompanies(any(ProspectCriteria.class), anyInt()))
            .thenReturn(domains);
        
        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);
        
        // Then
        assertEquals(2, result.size()); // Empty domain should be skipped
    }

    @Test
    void testEnrichProspectsWithHooks_WithEmptyList_ReturnsEmptyList() {
        // Given
        List<Prospect> prospects = new ArrayList<>();
        
        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(prospects, testSeller);
        
        // Then
        assertTrue(result.isEmpty());
        verify(aiServiceClient, never()).generatePersonalizationHook(any(), any());
    }
}

