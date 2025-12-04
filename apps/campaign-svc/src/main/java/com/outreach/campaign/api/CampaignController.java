package com.outreach.campaign.api;

import com.outreach.campaign.application.CampaignService;
import com.outreach.campaign.application.CsvParserService;
import com.outreach.campaign.application.LeadImportService;
import com.outreach.campaign.domain.Campaign;
import com.outreach.campaign.domain.Lead;
import com.outreach.campaign.domain.Company;
import com.outreach.campaign.domain.Contact;
import com.outreach.campaign.infrastructure.CompanyRepository;
import com.outreach.campaign.infrastructure.ContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CampaignController {
    private final CampaignService campaignService;
    private final CsvParserService csvParserService;
    private final LeadImportService leadImportService;
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    
    public CampaignController(
        CampaignService campaignService, 
        CsvParserService csvParserService,
        LeadImportService leadImportService,
        ContactRepository contactRepository,
        CompanyRepository companyRepository
    ) {
        this.campaignService = campaignService;
        this.csvParserService = csvParserService;
        this.leadImportService = leadImportService;
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
    
    @PostMapping("/campaigns/upload-csv")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("campaignName") String campaignName,
            @RequestParam(value = "description", required = false) String description) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
            }
            
            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File must be a CSV file"));
            }
            
            // Parse CSV
            List<Lead> leads = csvParserService.parseCsv(file);
            
            if (leads.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No valid leads found in CSV file"));
            }
            
            // Validate campaign name
            if (campaignName == null || campaignName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Campaign name is required"));
            }
            
            // Create campaign (saves to database)
            String name = campaignName.trim();
            
            Campaign campaign = campaignService.createCampaign(name, description, leads);
            
            // Import leads to database (companies and contacts tables)
            // Use campaign ID from the created campaign
            Integer campaignIdForImport = Integer.parseInt(campaign.id());
            List<Map<String, Object>> importedLeads = leadImportService.importLeadsToDatabase(leads, campaignIdForImport);
            
            Map<String, Object> response = new HashMap<>();
            response.put("campaign", campaign);
            response.put("leadsCount", leads.size());
            response.put("importedLeads", importedLeads);
            response.put("message", "CSV uploaded and campaign created successfully. Leads imported to database.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process CSV: " + e.getMessage()));
        }
    }
    
    @GetMapping("/campaigns")
    public List<Campaign> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }
    
    @GetMapping("/campaigns/{id}")
    public ResponseEntity<Campaign> getCampaign(@PathVariable String id) {
        return campaignService.getCampaignById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/campaigns/{id}/leads")
    public List<Lead> getCampaignLeads(@PathVariable String id) {
        return campaignService.getCampaignLeads(id);
    }
    
    @GetMapping("/campaigns/{campaignId}/contacts")
    public ResponseEntity<?> getCampaignContacts(@PathVariable Integer campaignId) {
        try {
            // Get all contacts (for now, we'll return all contacts since we don't have campaign-contact mapping yet)
            // TODO: Add campaign_contacts junction table to properly map contacts to campaigns
            List<Contact> allContacts = contactRepository.findAll();
            
            // Enrich contacts with company information
            List<Map<String, Object>> enrichedContacts = new ArrayList<>();
            for (Contact contact : allContacts) {
                Map<String, Object> contactMap = new HashMap<>();
                contactMap.put("contactId", contact.getContactId());
                contactMap.put("firstName", contact.getFirstName());
                contactMap.put("lastName", contact.getLastName());
                contactMap.put("jobTitle", contact.getJobTitle());
                contactMap.put("email", contact.getEmail());
                contactMap.put("personalizationNotes", contact.getPersonalizationNotes());
                contactMap.put("companyId", contact.getCompanyId());
                
                // Fetch company information
                if (contact.getCompanyId() != null) {
                    Company company = companyRepository.findById(contact.getCompanyId()).orElse(null);
                    if (company != null) {
                        contactMap.put("companyName", company.getName());
                        contactMap.put("companyIndustry", company.getIndustry());
                        contactMap.put("companyWebsite", company.getWebsite());
                        contactMap.put("companySize", company.getEmployeeCount());
                        contactMap.put("companyLocation", company.getHqLocation());
                    }
                }
                
                enrichedContacts.add(contactMap);
            }
            
            return ResponseEntity.ok(enrichedContacts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get contacts: " + e.getMessage()));
        }
    }
    
    @PutMapping("/campaigns/{id}/status")
    public ResponseEntity<Campaign> updateCampaignStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            Campaign.CampaignStatus status = Campaign.CampaignStatus.valueOf(
                request.get("status").toUpperCase());
            Campaign updated = campaignService.updateCampaignStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
}

