package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    @Mock
    private RestTemplate restTemplate;

    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        aiServiceClient = new AiServiceClient("http://test-ai", restTemplateBuilder);
    }

    @Test
    void testGenerateMatchingCompanies_Success() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(null, null, "Tech", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        Map<String, Object> responseBody = Map.of(
            "success", true,
            "company_domains", List.of("example.com", "other.com")
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
            eq("http://test-ai/api/v1/prospects/discover"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // When
        List<String> domains = aiServiceClient.generateMatchingCompanies(criteria, 5);

        // Then
        assertEquals(2, domains.size());
        assertTrue(domains.contains("example.com"));
    }

    @Test
    void testGenerateMatchingCompanies_Failure() {
        ProspectCriteria criteria = new ProspectCriteria(null, null, "Tech", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        List<String> domains = aiServiceClient.generateMatchingCompanies(criteria, 5);
        assertTrue(domains.isEmpty());
    }

    @Test
    void testGeneratePersonalizationHook_Success() {
        Prospect p = new Prospect("1", "Co", "A", "B", "CEO", "a@b.com", "b.com", "Tech", null, null, null, null, null);
        SellerProfile s = new SellerProfile("MyCo", "Tech", 100, 2020, "US", List.of(), "B2B", "High", List.of(), "Direct", List.of());

        Map<String, Object> responseBody = Map.of(
            "success", true,
            "hook", "Custom Hook"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
            eq("http://test-ai/api/v1/prospects/personalize"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        String hook = aiServiceClient.generatePersonalizationHook(p, s);
        assertEquals("Custom Hook", hook);
    }

    @Test
    void testGeneratePersonalizationHook_Fallback() {
        Prospect p = new Prospect("1", "Co", "A", "B", "CEO", "a@b.com", "b.com", "Tech", null, null, null, null, null);
        SellerProfile s = new SellerProfile("MyCo", "Tech", 100, 2020, "US", List.of(), "B2B", "High", List.of(), "Direct", List.of());

        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        String hook = aiServiceClient.generatePersonalizationHook(p, s);
        assertTrue(hook.contains("I noticed Co is in Tech"));
    }
}
