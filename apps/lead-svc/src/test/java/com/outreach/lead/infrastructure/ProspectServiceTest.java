package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.SellerProfile;
import com.outreach.lead.domain.ProspectCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProspectServiceTest {

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private RabbitMQClient rabbitMQClient;

    private ProspectService prospectService;

    @BeforeEach
    void setUp() {
        prospectService = new ProspectService(aiServiceClient, rabbitMQClient);
    }

    @Test
    void testEnrichProspectsWithHooks() {
        // Given
        Prospect p = new Prospect("1", "Co", "A", "B", "CEO", "a@b.com", "b.com", "Tech", 100, List.of("US"), List.of(), List.of(), null);
        SellerProfile s = new SellerProfile("MyCo", "Tech", 100, 2020, "US", List.of(), "B2B", "High", List.of(), "Direct", List.of());
        
        when(aiServiceClient.generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class)))
            .thenReturn("Generated Hook");

        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(List.of(p), s);

        // Then
        assertEquals(1, result.size());
        assertEquals("Generated Hook", result.get(0).personalizationHook());
    }

    @Test
    void testEnrichProspectsWithHooks_Failure() {
        // Given
        Prospect p = new Prospect("1", "Co", "A", "B", "CEO", "a@b.com", "b.com", "Tech", 100, List.of("US"), List.of(), List.of(), null);
        SellerProfile s = new SellerProfile("MyCo", "Tech", 100, 2020, "US", List.of(), "B2B", "High", List.of(), "Direct", List.of());
        
        when(aiServiceClient.generatePersonalizationHook(any(Prospect.class), any(SellerProfile.class)))
            .thenThrow(new RuntimeException("AI Error"));

        // When
        List<Prospect> result = prospectService.enrichProspectsWithHooks(List.of(p), s);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).personalizationHook().contains("I noticed Co is in Tech"));
    }

    @Test
    void testSearchProspects_FallbackToHttp() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(null, null, "Tech", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        // Mock deprecated method call path or whatever path it takes
        // The service defaults useRabbitMQ=true, but inside searchProspects, if useRabbitMQ is true, 
        // it calls aiServiceClient.generateMatchingCompanies fallback because the 'async' part is not supported in 'searchProspects'.
        
        when(aiServiceClient.generateMatchingCompanies(any(), anyInt()))
            .thenReturn(List.of("example.com"));

        // When
        List<Prospect> result = prospectService.searchProspects(criteria, 5);

        // Then
        assertEquals(1, result.size());
        assertEquals("example.com", result.get(0).domain());
        assertEquals("Example", result.get(0).company());
    }

    @Test
    void testSearchProspects_Empty() {
         ProspectCriteria criteria = new ProspectCriteria(null, null, "Tech", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

         when(aiServiceClient.generateMatchingCompanies(any(), anyInt()))
            .thenReturn(Collections.emptyList());

         List<Prospect> result = prospectService.searchProspects(criteria, 5);
         assertTrue(result.isEmpty());
    }
}
