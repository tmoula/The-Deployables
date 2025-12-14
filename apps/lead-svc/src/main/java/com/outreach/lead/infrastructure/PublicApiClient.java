package com.outreach.lead.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Client for fetching data from public APIs
 * Supports various public APIs for company and lead enrichment
 */
@Component
public class PublicApiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${public.api.enabled:false}")
    private boolean publicApiEnabled;
    
    @Value("${public.api.base.url:}")
    private String publicApiBaseUrl;
    
    @Value("${public.api.key:}")
    private String publicApiKey;
    
    @Value("${public.api.timeout:10000}")
    private int timeout;
    
    public PublicApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetch company data from public API
     * @param companyDomain Company domain (e.g., "example.com")
     * @return Map containing company data
     */
    public Map<String, Object> fetchCompanyData(String companyDomain) {
        if (!publicApiEnabled || publicApiBaseUrl == null || publicApiBaseUrl.isEmpty()) {
            System.out.println("Public API is not enabled or configured");
            return new HashMap<>();
        }
        
        try {
            System.out.println("Fetching company data from public API for domain: " + companyDomain);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Example endpoint: /companies?domain=example.com
            String url = publicApiBaseUrl + "/companies?domain=" + companyDomain;
            System.out.println("Calling public API at: " + url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("Successfully fetched company data from public API");
                return response.getBody();
            }
            
        } catch (RestClientException e) {
            System.err.println("Error fetching company data from public API: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error fetching company data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    /**
     * Fetch contact/lead data from public API
     * @param companyDomain Company domain
     * @param firstName First name (optional)
     * @param lastName Last name (optional)
     * @return Map containing contact data
     */
    public Map<String, Object> fetchContactData(String companyDomain, String firstName, String lastName) {
        if (!publicApiEnabled || publicApiBaseUrl == null || publicApiBaseUrl.isEmpty()) {
            System.out.println("Public API is not enabled or configured");
            return new HashMap<>();
        }
        
        try {
            System.out.println("Fetching contact data from public API for: " + firstName + " " + lastName + " at " + companyDomain);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Build query parameters
            StringBuilder urlBuilder = new StringBuilder(publicApiBaseUrl + "/contacts?domain=" + companyDomain);
            if (firstName != null && !firstName.isEmpty()) {
                urlBuilder.append("&first_name=").append(firstName);
            }
            if (lastName != null && !lastName.isEmpty()) {
                urlBuilder.append("&last_name=").append(lastName);
            }
            
            String url = urlBuilder.toString();
            System.out.println("Calling public API at: " + url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("Successfully fetched contact data from public API");
                return response.getBody();
            }
            
        } catch (RestClientException e) {
            System.err.println("Error fetching contact data from public API: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error fetching contact data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    /**
     * Generic method to fetch data from any public API endpoint
     * @param endpoint API endpoint (e.g., "/companies", "/contacts")
     * @param queryParams Map of query parameters
     * @return Map containing response data
     */
    public Map<String, Object> fetchData(String endpoint, Map<String, String> queryParams) {
        if (!publicApiEnabled || publicApiBaseUrl == null || publicApiBaseUrl.isEmpty()) {
            System.out.println("Public API is not enabled or configured");
            return new HashMap<>();
        }
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Build URL with query parameters
            StringBuilder urlBuilder = new StringBuilder(publicApiBaseUrl);
            if (!endpoint.startsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(endpoint);
            
            if (queryParams != null && !queryParams.isEmpty()) {
                urlBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, String> param : queryParams.entrySet()) {
                    if (!first) {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(param.getKey()).append("=").append(param.getValue());
                    first = false;
                }
            }
            
            String url = urlBuilder.toString();
            System.out.println("Calling public API at: " + url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("Successfully fetched data from public API");
                return response.getBody();
            }
            
        } catch (RestClientException e) {
            System.err.println("Error fetching data from public API: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error fetching data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    /**
     * POST data to public API
     * @param endpoint API endpoint
     * @param requestBody Request body as Map
     * @return Map containing response data
     */
    public Map<String, Object> postData(String endpoint, Map<String, Object> requestBody) {
        if (!publicApiEnabled || publicApiBaseUrl == null || publicApiBaseUrl.isEmpty()) {
            System.out.println("Public API is not enabled or configured");
            return new HashMap<>();
        }
        
        try {
            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            StringBuilder urlBuilder = new StringBuilder(publicApiBaseUrl);
            if (!endpoint.startsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(endpoint);
            
            String url = urlBuilder.toString();
            System.out.println("POSTing to public API at: " + url);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("Successfully posted data to public API");
                return response.getBody();
            }
            
        } catch (Exception e) {
            System.err.println("Error posting data to public API: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    /**
     * Create HTTP headers with authentication if API key is configured
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        // Add API key if configured
        if (publicApiKey != null && !publicApiKey.isEmpty()) {
            // Common patterns for API keys:
            // - Authorization: Bearer <token>
            // - X-API-Key: <key>
            // - Authorization: <key>
            headers.set("Authorization", "Bearer " + publicApiKey);
            headers.set("X-API-Key", publicApiKey);
        }
        
        return headers;
    }
    
    /**
     * Check if public API is enabled and configured
     */
    public boolean isEnabled() {
        return publicApiEnabled && 
               publicApiBaseUrl != null && 
               !publicApiBaseUrl.isEmpty();
    }
}

