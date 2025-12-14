package com.outreach.lead.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.outreach.lead.infrastructure.PublicApiClient;
import java.util.Map;
import java.util.HashMap;

/**
 * REST Controller for public API integration endpoints
 */
@RestController
@RequestMapping("/api/v1/public-api")
@CrossOrigin(origins = "*")
public class PublicApiController {
    
    private final PublicApiClient publicApiClient;
    
    @Autowired
    public PublicApiController(PublicApiClient publicApiClient) {
        this.publicApiClient = publicApiClient;
    }
    
    /**
     * Get company data from public API
     * GET /api/v1/public-api/company?domain=example.com
     */
    @GetMapping("/company")
    public ResponseEntity<Map<String, Object>> getCompanyData(
            @RequestParam String domain) {
        
        if (domain == null || domain.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Domain parameter is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> companyData = publicApiClient.fetchCompanyData(domain);
        
        if (companyData.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No data found or public API is not configured");
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", companyData);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get contact data from public API
     * GET /api/v1/public-api/contact?domain=example.com&firstName=John&lastName=Doe
     */
    @GetMapping("/contact")
    public ResponseEntity<Map<String, Object>> getContactData(
            @RequestParam String domain,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        
        if (domain == null || domain.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Domain parameter is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> contactData = publicApiClient.fetchContactData(domain, firstName, lastName);
        
        if (contactData.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No data found or public API is not configured");
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", contactData);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generic endpoint to fetch data from public API
     * GET /api/v1/public-api/data?endpoint=/companies&domain=example.com
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(
            @RequestParam String endpoint,
            @RequestParam Map<String, String> queryParams) {
        
        if (endpoint == null || endpoint.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Endpoint parameter is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Remove endpoint from queryParams if present
        queryParams.remove("endpoint");
        
        Map<String, Object> data = publicApiClient.fetchData(endpoint, queryParams);
        
        if (data.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No data found or public API is not configured");
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST data to public API
     * POST /api/v1/public-api/data
     * Body: {"endpoint": "/search", "body": {...}}
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> postData(
            @RequestBody Map<String, Object> request) {
        
        String endpoint = (String) request.get("endpoint");
        if (endpoint == null || endpoint.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Endpoint is required in request body");
            return ResponseEntity.badRequest().body(error);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) request.get("body");
        if (requestBody == null) {
            requestBody = new HashMap<>();
        }
        
        Map<String, Object> data = publicApiClient.postData(endpoint, requestBody);
        
        if (data.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No response received or public API is not configured");
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if public API is enabled and configured
     * GET /api/v1/public-api/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", publicApiClient.isEnabled());
        return ResponseEntity.ok(response);
    }
}

