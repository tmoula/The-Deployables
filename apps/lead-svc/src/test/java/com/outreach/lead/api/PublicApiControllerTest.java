package com.outreach.lead.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.lead.infrastructure.PublicApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicApiController.class)
class PublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicApiClient publicApiClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Default mock behavior - API enabled
        when(publicApiClient.isEnabled()).thenReturn(true);
    }

    @Test
    void testGetCompanyData_WithValidDomain_ReturnsCompanyData() throws Exception {
        // Given
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("name", "Example Inc");
        mockData.put("domain", "example.com");
        mockData.put("industry", "Technology");
        
        when(publicApiClient.fetchCompanyData("example.com")).thenReturn(mockData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/company")
                .param("domain", "example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Example Inc"))
            .andExpect(jsonPath("$.data.domain").value("example.com"));
    }

    @Test
    void testGetCompanyData_WithMissingDomain_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/company"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Domain parameter is required"));
    }

    @Test
    void testGetCompanyData_WithEmptyDomain_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/company")
                .param("domain", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCompanyData_WhenApiReturnsEmpty_ReturnsSuccessWithMessage() throws Exception {
        // Given
        when(publicApiClient.fetchCompanyData("example.com")).thenReturn(new HashMap<>());
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/company")
                .param("domain", "example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetContactData_WithValidParams_ReturnsContactData() throws Exception {
        // Given
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("email", "john.doe@example.com");
        mockData.put("phone", "+1-555-1234");
        
        when(publicApiClient.fetchContactData("example.com", "John", "Doe")).thenReturn(mockData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/contact")
                .param("domain", "example.com")
                .param("firstName", "John")
                .param("lastName", "Doe"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));
    }

    @Test
    void testGetContactData_WithOnlyDomain_Works() throws Exception {
        // Given
        Map<String, Object> mockData = new HashMap<>();
        when(publicApiClient.fetchContactData("example.com", null, null)).thenReturn(mockData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/contact")
                .param("domain", "example.com"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetContactData_WithMissingDomain_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/contact"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Domain parameter is required"));
    }

    @Test
    void testGetData_WithValidEndpoint_ReturnsData() throws Exception {
        // Given
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("results", "data");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("domain", "example.com");
        
        when(publicApiClient.fetchData("/companies", queryParams)).thenReturn(mockData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/data")
                .param("endpoint", "/companies")
                .param("domain", "example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.results").value("data"));
    }

    @Test
    void testGetData_WithMissingEndpoint_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/data"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Endpoint parameter is required"));
    }

    @Test
    void testPostData_WithValidRequest_ReturnsData() throws Exception {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("endpoint", "/search");
        Map<String, Object> bodyData = new HashMap<>();
        bodyData.put("query", "test");
        requestBody.put("body", bodyData);
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("success", true);
        
        when(publicApiClient.postData("/search", bodyData)).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/v1/public-api/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testPostData_WithMissingEndpoint_ReturnsBadRequest() throws Exception {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("body", new HashMap<>());
        
        // When & Then
        mockMvc.perform(post("/api/v1/public-api/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Endpoint is required in request body"));
    }

    @Test
    void testPostData_WithNullBody_UsesEmptyMap() throws Exception {
        // Given
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("endpoint", "/search");
        
        Map<String, Object> mockResponse = new HashMap<>();
        when(publicApiClient.postData("/search", new HashMap<>())).thenReturn(mockResponse);
        
        // When & Then
        mockMvc.perform(post("/api/v1/public-api/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk());
    }

    @Test
    void testGetStatus_WhenApiEnabled_ReturnsEnabled() throws Exception {
        // Given
        when(publicApiClient.isEnabled()).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testGetStatus_WhenApiDisabled_ReturnsDisabled() throws Exception {
        // Given
        when(publicApiClient.isEnabled()).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/v1/public-api/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void testGetData_EndpointRemovedFromQueryParams() throws Exception {
        // Given
        Map<String, Object> mockData = new HashMap<>();
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("domain", "example.com");
        
        when(publicApiClient.fetchData("/companies", expectedParams)).thenReturn(mockData);
        
        // When & Then - endpoint should not be in query params passed to client
        mockMvc.perform(get("/api/v1/public-api/data")
                .param("endpoint", "/companies")
                .param("domain", "example.com"))
            .andExpect(status().isOk());
    }
}

