package com.outreach.lead.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiServiceClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(aiServiceClient, "aiServiceUrl", "http://test-ai-service:8090");
    }

    @Test
    void testGenerateMatchingCompanies_WithValidResponse_ReturnsDomains() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", 50, 200, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        List<String> domains = List.of("example.com", "test.com");
        responseBody.put("company_domains", domains);
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        List<String> result = aiServiceClient.generateMatchingCompanies(criteria, 5);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("example.com"));
    }

    @Test
    void testGenerateMatchingCompanies_WithNullResponse_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        List<String> result = aiServiceClient.generateMatchingCompanies(criteria, 5);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGenerateMatchingCompanies_WithErrorInResponse_ThrowsException() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", "AI service error");
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            aiServiceClient.generateMatchingCompanies(criteria, 5);
        });
    }

    @Test
    void testGenerateMatchingCompanies_WithSuccessFalse_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        List<String> result = aiServiceClient.generateMatchingCompanies(criteria, 5);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGenerateMatchingCompanies_WithException_ReturnsEmptyList() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Technology", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
        );
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenThrow(new RestClientException("Network error"));
        
        // When
        List<String> result = aiServiceClient.generateMatchingCompanies(criteria, 5);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGeneratePersonalizationHook_WithValidResponse_ReturnsHook() {
        // Given
        Prospect prospect = new Prospect(
            "1", "Example Corp", "John", "Doe", "CTO",
            "john@example.com", "example.com", "Technology", 100,
            List.of("US"), List.of("Java"), List.of("cloud"), null
        );
        
        SellerProfile seller = new SellerProfile(
            "Seller Corp", "Technology", 200, 2020, "US",
            List.of("AI"), "Enterprise", "Premium", List.of("Python"),
            "Outbound", List.of("US", "EU")
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("hook", "I noticed Example Corp is using Java");
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        String result = aiServiceClient.generatePersonalizationHook(prospect, seller);
        
        // Then
        assertNotNull(result);
        assertEquals("I noticed Example Corp is using Java", result);
    }

    @Test
    void testGeneratePersonalizationHook_WithError_ReturnsFallback() {
        // Given
        Prospect prospect = new Prospect(
            "1", "Example Corp", "John", "Doe", "CTO",
            "john@example.com", "example.com", "Technology", 100,
            List.of("US"), List.of("Java"), List.of("cloud"), null
        );
        
        SellerProfile seller = new SellerProfile(
            "Seller Corp", "Technology", 200, 2020, "US",
            List.of("AI"), "Enterprise", "Premium", List.of("Python"),
            "Outbound", List.of("US", "EU")
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", "Service error");
        
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        String result = aiServiceClient.generatePersonalizationHook(prospect, seller);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Example Corp"));
    }

    @Test
    void testGeneratePersonalizationHook_WithException_ReturnsFallback() {
        // Given
        Prospect prospect = new Prospect(
            "1", "Example Corp", "John", "Doe", "CTO",
            "john@example.com", "example.com", "Technology", 100,
            List.of("US"), List.of("Java"), List.of("cloud"), null
        );
        
        SellerProfile seller = new SellerProfile(
            "Seller Corp", "Technology", 200, 2020, "US",
            List.of("AI"), "Enterprise", "Premium", List.of("Python"),
            "Outbound", List.of("US", "EU")
        );
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenThrow(new RuntimeException("Network error"));
        
        // When
        String result = aiServiceClient.generatePersonalizationHook(prospect, seller);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Example Corp"));
    }

    @Test
    void testGenerateMatchingCompanies_WithMultipleCriteriaFields() {
        // Given - using criteria with multiple fields populated (simpler version)
        ProspectCriteria criteria = new ProspectCriteria(
            "TestCompany", "test.com", "Technology", 50, 200, null, null,
            null, null, null, List.of("Java", "AWS"),
            null, List.of("CTO"), "Senior",
            "North America", null, null, List.of("US"),
            null, null, List.of("cloud"), null, null
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("company_domains", List.of("example.com"));
        
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
            .thenReturn(response);
        
        // When
        List<String> result = aiServiceClient.generateMatchingCompanies(criteria, 5);
        
        // Then
        assertFalse(result.isEmpty());
    }
}

