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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class GeminiApiClient {
    private final String apiKey;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiApiClient(
        @Value("${gemini.api.key}") String apiKey,
        @Value("${gemini.api.url}") String apiUrl
    ) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generate matching companies based on prospect criteria using Gemini AI
     * @param criteria User-provided search criteria
     * @param maxCompanies Maximum number of companies to return
     * @return List of company domains
     */
    public List<String> generateMatchingCompanies(com.outreach.lead.domain.ProspectCriteria criteria, int maxCompanies) {
        List<String> companies = new ArrayList<>();
        
        try {
            System.out.println("=== GEMINI API CLIENT: Starting company generation ===");
            System.out.println("Max companies requested: " + maxCompanies);
            
            // Build prompt for Gemini
            String prompt = buildPrompt(criteria, maxCompanies);
            
            System.out.println("=== Calling Gemini API ===");
            System.out.println("Prompt length: " + prompt.length());
            System.out.println("Prompt preview: " + (prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt));
            
            // Build request body - correct structure for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contentsList = new ArrayList<>();
            Map<String, Object> contents = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            contents.put("parts", parts);
            contentsList.add(contents);
            requestBody.put("contents", contentsList);
            
            // Build URL with API key
            String url = apiUrl + "?key=" + apiKey;
            System.out.println("Gemini API URL: " + apiUrl.replace(apiKey, "***"));
            
            // Make POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            System.out.println("Request body size: " + jsonBody.length() + " chars");
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            System.out.println("Making POST request to Gemini API...");
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            System.out.println("Gemini API response status: " + response.getStatusCode());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            
            if (responseBody == null) {
                System.err.println("Gemini API returned null response");
                return companies;
            }
            
            // Check for errors
            if (responseBody.containsKey("error")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                String errorMessage = error != null ? error.toString() : "Unknown error";
                System.err.println("=== GEMINI API ERROR ===");
                System.err.println("Error object: " + errorMessage);
                System.err.println("Full response: " + responseBody);
                throw new RuntimeException("Gemini API Error: " + errorMessage);
            }
            
            // Extract text from Gemini response
            String generatedText = extractTextFromResponse(responseBody);
            System.out.println("Gemini generated text: " + generatedText);
            
            if (generatedText == null || generatedText.isEmpty()) {
                System.err.println("Gemini API returned empty text");
                return companies;
            }
            
            // Parse company domains from the response
            companies = parseCompanyDomains(generatedText, maxCompanies);
            System.out.println("Parsed " + companies.size() + " company domains from Gemini response: " + companies);
            
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
        }
        
        return companies;
    }

    /**
     * Build prompt for Gemini AI based on criteria
     */
    private String buildPrompt(com.outreach.lead.domain.ProspectCriteria criteria, int maxCompanies) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a business intelligence assistant. Based on the following criteria, provide exactly ");
        prompt.append(maxCompanies);
        prompt.append(" company domain names (just the domain, like example.com) that match these criteria. Make sure to provide exactly ").append(maxCompanies).append(" companies:\n\n");
        
        prompt.append("Criteria:\n");
        
        if (criteria.industry() != null && !criteria.industry().isEmpty()) {
            prompt.append("- Industry: ").append(criteria.industry()).append("\n");
        }
        
        if (criteria.minSize() != null || criteria.maxSize() != null) {
            prompt.append("- Company Size: ");
            if (criteria.minSize() != null) prompt.append(criteria.minSize());
            prompt.append(" - ");
            if (criteria.maxSize() != null) prompt.append(criteria.maxSize());
            prompt.append(" employees\n");
        }
        
        if (criteria.hqCountry() != null && !criteria.hqCountry().isEmpty()) {
            prompt.append("- Country: ").append(criteria.hqCountry()).append("\n");
        } else if (criteria.headquartersRegion() != null && !criteria.headquartersRegion().isEmpty()) {
            prompt.append("- Region: ").append(criteria.headquartersRegion()).append("\n");
        }
        
        if (criteria.regions() != null && !criteria.regions().isEmpty()) {
            prompt.append("- Target Regions: ").append(String.join(", ", criteria.regions())).append("\n");
        }
        
        if (criteria.techUsed() != null && !criteria.techUsed().isEmpty()) {
            prompt.append("- Technology Used: ").append(String.join(", ", criteria.techUsed())).append("\n");
        }
        
        if (criteria.keywordMentions() != null && !criteria.keywordMentions().isEmpty()) {
            prompt.append("- Keywords: ").append(String.join(", ", criteria.keywordMentions())).append("\n");
        }
        
        prompt.append("\n");
        prompt.append("Return ONLY the domain names, one per line, in this exact format:\n");
        prompt.append("domain1.com\n");
        prompt.append("domain2.com\n");
        prompt.append("domain3.com\n");
        prompt.append("...\n");
        prompt.append("\n");
        prompt.append("Provide at least ").append(maxCompanies).append(" company domains. Do not include any explanations, just the domain names.");
        
        return prompt.toString();
    }

    /**
     * Extract text from Gemini API response
     */
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "";
            }
            
            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            if (content == null) {
                return "";
            }
            
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "";
            }
            
            Map<String, Object> part = parts.get(0);
            return (String) part.get("text");
        } catch (Exception e) {
            System.err.println("Error extracting text from Gemini response: " + e.getMessage());
            return "";
        }
    }

    /**
     * Parse company domains from Gemini's text response
     */
    private List<String> parseCompanyDomains(String text, int maxCompanies) {
        List<String> domains = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return domains;
        }
        
        // Pattern to match domain names (e.g., example.com, subdomain.example.com)
        Pattern domainPattern = Pattern.compile("([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}");
        Matcher matcher = domainPattern.matcher(text);
        
        while (matcher.find() && domains.size() < maxCompanies) {
            String domain = matcher.group().toLowerCase().trim();
            // Remove common prefixes
            domain = domain.replace("https://", "").replace("http://", "").replace("www.", "");
            // Remove trailing slashes or paths
            if (domain.contains("/")) {
                domain = domain.split("/")[0];
            }
            // Validate it's a proper domain
            if (domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".")) {
                domains.add(domain);
            }
        }
        
        return domains;
    }

    /**
     * Generate a personalization hook for a prospect based on company and seller information
     * @param prospect The prospect to generate a hook for
     * @param seller The seller's profile information
     * @return A personalized hook string for cold outreach
     */
    public String generatePersonalizationHook(com.outreach.lead.domain.Prospect prospect, com.outreach.lead.domain.SellerProfile seller) {
        try {
            System.out.println("=== GEMINI API CLIENT: Generating personalization hook ===");
            System.out.println("Prospect: " + prospect.company() + " - " + prospect.firstName() + " " + prospect.lastName());
            
            // Build prompt for personalization hook
            String prompt = buildPersonalizationPrompt(prospect, seller);
            
            System.out.println("=== Calling Gemini API for personalization hook ===");
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contentsList = new ArrayList<>();
            Map<String, Object> contents = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            contents.put("parts", parts);
            contentsList.add(contents);
            requestBody.put("contents", contentsList);
            
            // Build URL with API key
            String url = apiUrl + "?key=" + apiKey;
            
            // Make POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            
            if (responseBody == null || responseBody.containsKey("error")) {
                System.err.println("Gemini API returned error for personalization hook");
                return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
            }
            
            // Extract text from Gemini response
            String generatedText = extractTextFromResponse(responseBody);
            
            if (generatedText == null || generatedText.isEmpty()) {
                System.err.println("Gemini API returned empty text for personalization hook");
                return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
            }
            
            // Clean up the response (remove any quotes or extra formatting)
            String hook = generatedText.trim();
            if (hook.startsWith("\"") && hook.endsWith("\"")) {
                hook = hook.substring(1, hook.length() - 1);
            }
            
            System.out.println("Generated personalization hook: " + hook);
            return hook;
            
        } catch (Exception e) {
            System.err.println("Error generating personalization hook: " + e.getMessage());
            e.printStackTrace();
            // Return a fallback hook
            return "Hi " + prospect.firstName() + ", I noticed " + prospect.company() + " is in " + prospect.industry() + ".";
        }
    }

    /**
     * Build prompt for personalization hook generation - Research-based and specific
     */
    private String buildPersonalizationPrompt(com.outreach.lead.domain.Prospect prospect, com.outreach.lead.domain.SellerProfile seller) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a sales research expert. Generate a highly personalized, research-based opening hook (1-2 sentences, max 120 characters) for a cold email that shows deep knowledge of the prospect's company.\n\n");
        
        prompt.append("SELLER INFORMATION:\n");
        prompt.append("- Company: ").append(seller.companyName()).append("\n");
        prompt.append("- Industry: ").append(seller.industry()).append("\n");
        if (seller.valuePropositionKeywords() != null && !seller.valuePropositionKeywords().isEmpty()) {
            prompt.append("- Value Proposition: ").append(String.join(", ", seller.valuePropositionKeywords())).append("\n");
        }
        if (seller.targetCustomerSegment() != null) {
            prompt.append("- Target Segment: ").append(seller.targetCustomerSegment()).append("\n");
        }
        if (seller.techStack() != null && !seller.techStack().isEmpty()) {
            prompt.append("- Tech Stack: ").append(String.join(", ", seller.techStack())).append("\n");
        }
        
        prompt.append("\nPROSPECT INFORMATION:\n");
        prompt.append("- Name: ").append(prospect.firstName()).append(" ").append(prospect.lastName()).append("\n");
        prompt.append("- Position: ").append(prospect.position()).append("\n");
        prompt.append("- Company: ").append(prospect.company()).append("\n");
        prompt.append("- Domain: ").append(prospect.domain()).append("\n");
        prompt.append("- Industry: ").append(prospect.industry()).append("\n");
        if (prospect.size() != null) {
            prompt.append("- Company Size: ").append(prospect.size()).append(" employees\n");
        }
        if (prospect.regions() != null && !prospect.regions().isEmpty()) {
            prompt.append("- Regions: ").append(String.join(", ", prospect.regions())).append("\n");
        }
        if (prospect.stack() != null && !prospect.stack().isEmpty()) {
            prompt.append("- Tech Stack: ").append(String.join(", ", prospect.stack())).append("\n");
        }
        if (prospect.keywords() != null && !prospect.keywords().isEmpty()) {
            prompt.append("- Keywords/Interests: ").append(String.join(", ", prospect.keywords())).append("\n");
        }
        
        prompt.append("\n");
        prompt.append("Based on the company name '").append(prospect.company()).append("' and domain '").append(prospect.domain()).append("', generate a hook that includes:\n");
        prompt.append("1. A SPECIFIC detail about what ").append(prospect.company()).append(" does or offers (their unique value proposition, main product/service, or market position)\n");
        prompt.append("2. Something that shows you researched them (their industry focus, company size implications, or regional presence)\n");
        prompt.append("3. A natural connection to how ").append(seller.companyName()).append("'s ").append(seller.valuePropositionKeywords() != null && !seller.valuePropositionKeywords().isEmpty() ? String.join(", ", seller.valuePropositionKeywords()) : "solutions").append(" could help them\n");
        prompt.append("\n");
        prompt.append("Examples of good hooks:\n");
        prompt.append("- \"Noticed ").append(prospect.company()).append(" specializes in [specific offering] - our AI automation helps companies like yours [specific benefit]\"\n");
        prompt.append("- \"").append(prospect.company()).append("'s focus on [specific area] aligns perfectly with how we help [target segment] [achieve specific outcome]\"\n");
        prompt.append("- \"As ").append(prospect.company()).append(" scales to ").append(prospect.size() != null ? prospect.size() : "your size").append(" employees, [specific pain point] becomes critical - we solve this by [specific solution]\"\n");
        prompt.append("\n");
        prompt.append("Requirements:\n");
        prompt.append("- Be SPECIFIC about the company's offerings or market position (not generic)\n");
        prompt.append("- Show research depth (mention their industry, size, or unique characteristics)\n");
        prompt.append("- Connect naturally to seller's value proposition\n");
        prompt.append("- 1-2 sentences, max 120 characters\n");
        prompt.append("- NO greetings (no 'Hi', 'Hello', etc.)\n");
        prompt.append("- Sound like you actually researched the company\n");
        prompt.append("\n");
        prompt.append("Return ONLY the hook text, nothing else. No quotes, no explanations, no prefixes.");
        
        return prompt.toString();
    }
}

