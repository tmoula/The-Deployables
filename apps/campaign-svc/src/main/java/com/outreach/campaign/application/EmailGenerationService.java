package com.outreach.campaign.application;

import com.outreach.campaign.domain.Company;
import com.outreach.campaign.domain.Contact;
import com.outreach.campaign.infrastructure.AiServiceClient;
import com.outreach.campaign.infrastructure.CompanyRepository;
import com.outreach.campaign.infrastructure.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class EmailGenerationService {
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final AiServiceClient aiServiceClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String leadServiceUrl;
    
    public EmailGenerationService(
        CompanyRepository companyRepository,
        ContactRepository contactRepository,
        AiServiceClient aiServiceClient,
        @org.springframework.beans.factory.annotation.Value("${lead.service.url:http://lead-svc:8081}") String leadServiceUrl
    ) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.aiServiceClient = aiServiceClient;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.leadServiceUrl = leadServiceUrl;
    }
    
    private Map<String, Object> getSellerProfile() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                leadServiceUrl + "/api/v1/seller",
                Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("Could not fetch seller profile: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> generateEmailForContact(
        Integer contactId,
        Integer campaignId,
        Map<String, Object> emailRequirements
    ) {
        // Fetch contact from database
        Contact contact = contactRepository.findById(contactId)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));
        
        // Fetch company from database
        Company company = companyRepository.findById(contact.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found for contact: " + contactId));
        
        // Build request for AI service
        Map<String, Object> request = new HashMap<>();
        
        // Company info from database
        Map<String, Object> companyInfo = new HashMap<>();
        companyInfo.put("company_id", company.getCompanyId());
        companyInfo.put("name", company.getName());
        companyInfo.put("website", company.getWebsite());
        companyInfo.put("industry", company.getIndustry());
        companyInfo.put("employee_count", company.getEmployeeCount());
        companyInfo.put("location", company.getHqLocation());
        companyInfo.put("enrichment_notes", company.getEnrichmentNotes());
        
        // Contact info from database
        Map<String, Object> contactInfo = new HashMap<>();
        contactInfo.put("contact_id", contact.getContactId());
        contactInfo.put("first_name", contact.getFirstName());
        contactInfo.put("last_name", contact.getLastName());
        contactInfo.put("job_title", contact.getJobTitle());
        contactInfo.put("email", contact.getEmail());
        contactInfo.put("personalization_notes", contact.getPersonalizationNotes());
        
        // Get seller profile from lead service
        Map<String, Object> sellerProfile = getSellerProfile();
        if (sellerProfile != null) {
            request.put("seller", sellerProfile);
        }
        
        request.put("company", companyInfo);
        request.put("contact", contactInfo);
        request.put("requirements", emailRequirements);
        request.put("sequence_step", 1);
        request.put("campaign_id", campaignId);
        
        // Call AI service
        return aiServiceClient.generateEmail(request);
    }
}

