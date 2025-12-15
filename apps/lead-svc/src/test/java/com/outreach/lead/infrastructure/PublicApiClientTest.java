package com.outreach.lead.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private PublicApiClient publicApiClient;

    @BeforeEach
    void setUp() {
        // PublicApiClient has a no-arg constructor that creates new instances
        publicApiClient = new PublicApiClient();
        ReflectionTestUtils.setField(publicApiClient, "restTemplate", restTemplate);
    }

    @Test
    void testFetchCompanyData_WhenApiDisabled_ReturnsEmptyMap() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", false);
        
        // When
        Map<String, Object> result = publicApiClient.fetchCompanyData("example.com");
        
        // Then
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), any(Class.class));
    }

    @Test
    void testFetchCompanyData_WhenBaseUrlEmpty_ReturnsEmptyMap() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "");
        
        // When
        Map<String, Object> result = publicApiClient.fetchCompanyData("example.com");
        
        // Then
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), any(Class.class));
    }

    @Test
    void testFetchCompanyData_WhenApiEnabled_ReturnsCompanyData() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        ReflectionTestUtils.setField(publicApiClient, "publicApiKey", "test-key");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("name", "Example Inc");
        mockResponse.put("domain", "example.com");
        mockResponse.put("industry", "Technology");
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchCompanyData("example.com");
        
        // Then
        assertFalse(result.isEmpty());
        assertEquals("Example Inc", result.get("name"));
        assertEquals("example.com", result.get("domain"));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchCompanyData_WhenApiThrowsException_ReturnsEmptyMap() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("API Error"));
        
        // When
        Map<String, Object> result = publicApiClient.fetchCompanyData("example.com");
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchContactData_WhenApiEnabled_ReturnsContactData() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("email", "john.doe@example.com");
        mockResponse.put("phone", "+1-555-1234");
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchContactData("example.com", "John", "Doe");
        
        // Then
        assertFalse(result.isEmpty());
        assertEquals("john.doe@example.com", result.get("email"));
    }

    @Test
    void testFetchContactData_WithNullNames_StillWorks() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> mockResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchContactData("example.com", null, null);
        
        // Then
        assertNotNull(result);
    }

    @Test
    void testFetchData_WithQueryParams_ReturnsData() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("domain", "example.com");
        queryParams.put("size", "500");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("results", "data");
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchData("/companies", queryParams);
        
        // Then
        assertFalse(result.isEmpty());
    }

    @Test
    void testFetchData_WithoutQueryParams_StillWorks() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> mockResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchData("/companies", null);
        
        // Then
        assertNotNull(result);
    }

    @Test
    void testPostData_WhenApiEnabled_ReturnsData() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", "search term");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("success", true);
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.postData("/search", requestBody);
        
        // Then
        assertFalse(result.isEmpty());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testPostData_WhenApiThrowsException_ReturnsEmptyMap() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> requestBody = new HashMap<>();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RuntimeException("Network error"));
        
        // When
        Map<String, Object> result = publicApiClient.postData("/search", requestBody);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testIsEnabled_WhenApiDisabled_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", false);
        
        // When
        boolean result = publicApiClient.isEnabled();
        
        // Then
        assertFalse(result);
    }

    @Test
    void testIsEnabled_WhenApiEnabledButNoBaseUrl_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "");
        
        // When
        boolean result = publicApiClient.isEnabled();
        
        // Then
        assertFalse(result);
    }

    @Test
    void testIsEnabled_WhenFullyConfigured_ReturnsTrue() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        // When
        boolean result = publicApiClient.isEnabled();
        
        // Then
        assertTrue(result);
    }

    @Test
    void testFetchCompanyData_WithApiKey_IncludesAuthHeaders() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        ReflectionTestUtils.setField(publicApiClient, "publicApiKey", "my-api-key");
        
        Map<String, Object> mockResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenAnswer(invocation -> {
                HttpEntity<?> entity = invocation.getArgument(2);
                HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                assertTrue(headers.containsKey("Authorization"));
                assertTrue(headers.containsKey("X-API-Key"));
                return responseEntity;
            });
        
        // When
        publicApiClient.fetchCompanyData("example.com");
        
        // Then
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchCompanyData_WhenResponseIsNull_ReturnsEmptyMap() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchCompanyData("example.com");
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchData_WithEndpointStartingWithSlash() {
        // Given
        ReflectionTestUtils.setField(publicApiClient, "publicApiEnabled", true);
        ReflectionTestUtils.setField(publicApiClient, "publicApiBaseUrl", "https://api.example.com/v1");
        
        Map<String, Object> mockResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
            .thenReturn(responseEntity);
        
        // When
        Map<String, Object> result = publicApiClient.fetchData("/companies", new HashMap<>());
        
        // Then
        assertNotNull(result);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class));
    }
}

