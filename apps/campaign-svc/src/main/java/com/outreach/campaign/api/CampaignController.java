package com.outreach.campaign.api;

import com.outreach.campaign.application.CampaignService;
import com.outreach.campaign.application.CsvParserService;
import com.outreach.campaign.application.LeadImportService;
import com.outreach.campaign.application.UserContextService;
import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.infrastructure.LeadRepository;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class CampaignController {
    private final CampaignService campaignService;
    private final CsvParserService csvParserService;
    private final LeadImportService leadImportService;
    private final UserContextService userContextService;
    private final LeadRepository leadRepository;
    
    public CampaignController(
        CampaignService campaignService,
        CsvParserService csvParserService,
        LeadImportService leadImportService,
        UserContextService userContextService,
        LeadRepository leadRepository
    ) {
        this.campaignService = campaignService;
        this.csvParserService = csvParserService;
        this.leadImportService = leadImportService;
        this.userContextService = userContextService;
        this.leadRepository = leadRepository;
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
    
    @PostMapping("/campaigns/upload-csv")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("campaignName") String campaignName,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            System.out.println("UPLOAD CSV - Received request");
            System.out.println("UPLOAD CSV - User Email Header: " + userEmail);
            System.out.println("UPLOAD CSV - Campaign Name: " + campaignName);
            System.out.println("UPLOAD CSV - File Name: " + (file != null ? file.getOriginalFilename() : "null"));
            System.out.println("UPLOAD CSV - File Size: " + (file != null ? file.getSize() : "null"));
            
            if (file.isEmpty()) {
                System.out.println("UPLOAD CSV - Error: File is empty");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
            }
            
            if (!file.getOriginalFilename().endsWith(".csv")) {
                System.out.println("UPLOAD CSV - Error: File is not CSV");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File must be a CSV file"));
            }
            
            // Parse CSV
            System.out.println("UPLOAD CSV - Parsing CSV file...");
            List<Lead> leads = csvParserService.parseCsv(file);
            System.out.println("UPLOAD CSV - Parsed " + leads.size() + " leads");
            
            if (leads.isEmpty()) {
                System.out.println("UPLOAD CSV - Error: No valid leads found");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No valid leads found in CSV file"));
            }
            
            // Validate campaign name
            if (campaignName == null || campaignName.trim().isEmpty()) {
                System.out.println("UPLOAD CSV - Error: Campaign name is required");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Campaign name is required"));
            }
            
            // Resolve user from email header
            System.out.println("UPLOAD CSV - Resolving user ID from email...");
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            System.out.println("UPLOAD CSV - Resolved User ID: " + userId);

            // Create campaign (saves to database) for this specific user
            String name = campaignName.trim();
            System.out.println("UPLOAD CSV - Creating campaign with name: " + name + " for user_id: " + userId);
            
            Campaign campaign = campaignService.createCampaign(userId, name, description, leads);
            System.out.println("UPLOAD CSV - Campaign created with ID: " + campaign.id());
            
            // Import leads to database (leads table)
            System.out.println("UPLOAD CSV - Importing leads to database for user_id: " + userId);
            List<Map<String, Object>> importedLeads = leadImportService.importLeadsToDatabase(leads, userId);
            System.out.println("UPLOAD CSV - Imported " + importedLeads.size() + " leads to database");
            
            Map<String, Object> response = new HashMap<>();
            response.put("campaign", campaign);
            response.put("leadsCount", leads.size());
            response.put("importedLeads", importedLeads);
            response.put("message", "CSV uploaded and campaign created successfully. " + importedLeads.size() + " leads imported to database.");
            
            System.out.println("UPLOAD CSV - Success! Returning response");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("UPLOAD CSV - Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process CSV: " + e.getMessage()));
        }
    }
    
    @GetMapping("/campaigns")
    public List<Campaign> getAllCampaigns(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        System.out.println("GET ALL CAMPAIGNS - User Email Header: " + userEmail);
        try {
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            System.out.println("GET ALL CAMPAIGNS - Resolved User ID: " + userId);
            List<Campaign> campaigns = campaignService.getAllCampaigns(userId);
            System.out.println("GET ALL CAMPAIGNS - Returning " + campaigns.size() + " campaigns");
            return campaigns;
        } catch (Exception e) {
            System.out.println("GET ALL CAMPAIGNS - Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
    public ResponseEntity<?> getCampaignContacts(
            @PathVariable Integer campaignId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Resolve user ID
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            
            // Get leads for this user (we can filter by campaign later using campaign_leads table)
            List<LeadEntity> leads = leadRepository.findByUserId(userId);
            
            // Convert to response format
            List<Map<String, Object>> enrichedContacts = new ArrayList<>();
            for (LeadEntity lead : leads) {
                Map<String, Object> contactMap = new HashMap<>();
                contactMap.put("contactId", lead.getId());
                contactMap.put("leadId", lead.getId());
                contactMap.put("firstName", lead.getFirstName());
                contactMap.put("lastName", lead.getLastName());
                contactMap.put("jobTitle", lead.getJobTitle());
                contactMap.put("email", lead.getEmail());
                contactMap.put("customNotes", lead.getCustomNotes());
                contactMap.put("companyName", lead.getCompanyName());
                contactMap.put("companyWebsite", lead.getCompanyWebsite());
                contactMap.put("companyLinkedin", lead.getCompanyLinkedin());
                contactMap.put("linkedinUrl", lead.getLinkedinUrl());
                contactMap.put("country", lead.getCountry());
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
    
    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<?> deleteCampaign(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            System.out.println("DELETE REQUEST - Campaign ID: " + id + ", User Email: " + userEmail);
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            System.out.println("DELETE REQUEST - Resolved User ID: " + userId);
            campaignService.deleteCampaign(id, userId);
            System.out.println("DELETE REQUEST - Successfully deleted campaign " + id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            System.out.println("DELETE REQUEST - Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.out.println("DELETE REQUEST - Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete campaign: " + e.getMessage()));
        }
    }
    
}

