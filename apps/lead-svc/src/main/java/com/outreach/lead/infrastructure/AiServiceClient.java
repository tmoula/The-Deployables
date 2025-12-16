package com.outreach.lead.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Component
public class AiServiceClient {
    private final String aiServiceUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(
        @Value("${ai.service.url:http://ai-svc:8090}") String aiServiceUrl,
        org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder
    ) {
        this.aiServiceUrl = aiServiceUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Generate matching companies based on prospect criteria using AI service
     * @param criteria User-provided search criteria
     * @param maxCompanies Maximum number of companies to return
     * @return List of company domains
     */
    public List<String> generateMatchingCompanies(com.outreach.lead.domain.ProspectCriteria criteria, int maxCompanies) {
        List<String> companies = new ArrayList<>();
        
        try {
            System.out.println("=== AI SERVICE CLIENT: Starting company generation ===");
            System.out.println("AI Service URL: " + aiServiceUrl);
            System.out.println("Max companies requested: " + maxCompanies);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> criteriaMap = new HashMap<>();
            
            // Map criteria fields
            if (criteria.industry() != null) criteriaMap.put("industry", criteria.industry());
            if (criteria.minSize() != null) criteriaMap.put("min_size", criteria.minSize());
            if (criteria.maxSize() != null) criteriaMap.put("max_size", criteria.maxSize());
            if (criteria.hqCountry() != null) criteriaMap.put("hq_country", criteria.hqCountry());
            if (criteria.headquartersRegion() != null) criteriaMap.put("headquarters_region", criteria.headquartersRegion());
            if (criteria.regions() != null) criteriaMap.put("regions", criteria.regions());
            if (criteria.techUsed() != null) criteriaMap.put("tech_used", criteria.techUsed());
            if (criteria.keywordMentions() != null) criteriaMap.put("keyword_mentions", criteria.keywordMentions());
            if (criteria.targetRoles() != null) criteriaMap.put("target_roles", criteria.targetRoles());
            if (criteria.seniorityLevel() != null) criteriaMap.put("seniority_level", criteria.seniorityLevel());
            
            requestBody.put("criteria", criteriaMap);
            requestBody.put("max_companies", maxCompanies);
            
            // Make POST request to AI service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            System.out.println("Request body: " + jsonBody);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            String url = aiServiceUrl + "/api/v1/prospects/discover";
            System.out.println("Calling AI service at: " + url);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            System.out.println("AI Service response status: " + response.getStatusCode());
            System.out.println("AI Service response headers: " + response.getHeaders());
            
            Map<String, Object> responseBody = response.getBody();
            System.out.println("AI Service response body: " + responseBody);
            
            if (responseBody == null) {
                System.err.println("=== ERROR: AI Service returned null response ===");
                return companies;
            }
            
            // Check for errors
            Object errorObj = responseBody.get("error");
            if (errorObj != null) {
                String errorMessage = errorObj.toString();
                System.err.println("=== AI SERVICE ERROR ===");
                System.err.println("Error: " + errorMessage);
                throw new RuntimeException("AI Service Error: " + errorMessage);
            }
            
            // Extract company domains
            Boolean success = (Boolean) responseBody.get("success");
            System.out.println("AI Service success flag: " + success);
            
            if (Boolean.TRUE.equals(success)) {
                @SuppressWarnings("unchecked")
                List<String> domains = (List<String>) responseBody.get("company_domains");
                System.out.println("AI Service company_domains: " + domains);
                System.out.println("AI Service company_domains is null: " + (domains == null));
                System.out.println("AI Service company_domains size: " + (domains != null ? domains.size() : 0));
                
                if (domains != null) {
                    companies.addAll(domains);
                    System.out.println("Added " + domains.size() + " domains to companies list");
                } else {
                    System.err.println("=== WARNING: company_domains is null despite success=true ===");
                }
            } else {
                System.err.println("=== WARNING: AI Service returned success=false ===");
            }
            
            System.out.println("=== FINAL RESULT ===");
            System.out.println("AI Service generated " + companies.size() + " company domains: " + companies);
            
        } catch (Exception e) {
            System.err.println("Error calling AI Service: " + e.getMessage());
            e.printStackTrace();
        }
        
        return companies;
    }

    /**
     * Generate a personalization hook for a prospect based on company and seller information
     * @param prospect The prospect to generate a hook for
     * @param seller The seller's profile information
     * @return A personalized hook string for cold outreach
     */
    public String generatePersonalizationHook(com.outreach.lead.domain.Prospect prospect, com.outreach.lead.domain.SellerProfile seller) {
        try {
            System.out.println("=== AI SERVICE CLIENT: Generating personalization hook ===");
            System.out.println("Prospect: " + prospect.company() + " - " + prospect.firstName() + " " + prospect.lastName());
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            
            // Map prospect
            Map<String, Object> prospectMap = new HashMap<>();
            if (prospect.id() != null) prospectMap.put("id", prospect.id());
            prospectMap.put("company", prospect.company());
            if (prospect.firstName() != null) prospectMap.put("first_name", prospect.firstName());
            if (prospect.lastName() != null) prospectMap.put("last_name", prospect.lastName());
            if (prospect.position() != null) prospectMap.put("position", prospect.position());
            if (prospect.email() != null) prospectMap.put("email", prospect.email());
            prospectMap.put("domain", prospect.domain());
            if (prospect.industry() != null) prospectMap.put("industry", prospect.industry());
            if (prospect.size() != null) prospectMap.put("size", prospect.size());
            if (prospect.regions() != null) prospectMap.put("regions", prospect.regions());
            if (prospect.stack() != null) prospectMap.put("stack", prospect.stack());
            if (prospect.keywords() != null) prospectMap.put("keywords", prospect.keywords());
            
            // Map seller
            Map<String, Object> sellerMap = new HashMap<>();
            sellerMap.put("company_name", seller.companyName());
            sellerMap.put("industry", seller.industry());
            if (seller.companySize() != null) sellerMap.put("company_size", seller.companySize());
            if (seller.foundedYear() != null) sellerMap.put("founded_year", seller.foundedYear());
            if (seller.headquartersRegion() != null) sellerMap.put("headquarters_region", seller.headquartersRegion());
            if (seller.valuePropositionKeywords() != null) sellerMap.put("value_proposition_keywords", seller.valuePropositionKeywords());
            if (seller.targetCustomerSegment() != null) sellerMap.put("target_customer_segment", seller.targetCustomerSegment());
            if (seller.priceTier() != null) sellerMap.put("price_tier", seller.priceTier());
            if (seller.techStack() != null) sellerMap.put("tech_stack", seller.techStack());
            if (seller.salesModel() != null) sellerMap.put("sales_model", seller.salesModel());
            if (seller.targetRegions() != null) sellerMap.put("target_regions", seller.targetRegions());
            
            requestBody.put("prospect", prospectMap);
            requestBody.put("seller", sellerMap);
            
            // Make POST request to AI service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            String url = aiServiceUrl + "/api/v1/prospects/personalize";
            System.out.println("Calling AI service at: " + url);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null || responseBody.containsKey("error")) {
                String errorMessage = responseBody != null ? responseBody.get("error").toString() : "Null response";
                System.err.println("AI Service returned error for personalization hook: " + errorMessage);
                return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
            }
            
            // Extract hook
            Boolean success = (Boolean) responseBody.get("success");
            if (Boolean.TRUE.equals(success)) {
                String hook = (String) responseBody.get("hook");
                if (hook != null && !hook.isEmpty()) {
                    System.out.println("Generated personalization hook: " + hook);
                    return hook;
                }
            }
            
            // Fallback
            return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
            
        } catch (Exception e) {
            System.err.println("Error generating personalization hook: " + e.getMessage());
            e.printStackTrace();
            // Return a fallback hook
            return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
        }
    }
}

