package com.outreach.campaign.api;

import com.outreach.campaign.application.CampaignLeadService;
import com.outreach.campaign.application.CampaignService;
import com.outreach.campaign.application.CsvParseResult;
import com.outreach.campaign.application.CsvParserService;
import com.outreach.campaign.application.EmailRenderService;
import com.outreach.campaign.application.LeadImportService;
import com.outreach.campaign.application.UserContextService;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.infrastructure.LeadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class CampaignController {
    private final CampaignService campaignService;
    private final CsvParserService csvParserService;
    private final LeadImportService leadImportService;
    private final UserContextService userContextService;
    private final LeadRepository leadRepository;
    private final CampaignLeadService campaignLeadService;
    private final EmailRenderService emailRenderService;
    private final ObjectMapper objectMapper;
    
    public CampaignController(
        CampaignService campaignService,
        CsvParserService csvParserService,
        LeadImportService leadImportService,
        UserContextService userContextService,
        LeadRepository leadRepository,
        CampaignLeadService campaignLeadService,
        EmailRenderService emailRenderService
    ) {
        this.campaignService = campaignService;
        this.csvParserService = csvParserService;
        this.leadImportService = leadImportService;
        this.userContextService = userContextService;
        this.leadRepository = leadRepository;
        this.campaignLeadService = campaignLeadService;
        this.emailRenderService = emailRenderService;
        this.objectMapper = new ObjectMapper();
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
            
            // Parse CSV - Extract ALL headers and ALL row data
            System.out.println("UPLOAD CSV - Parsing CSV file...");
            CsvParseResult parseResult = csvParserService.parseCsv(file);
            List<Lead> leads = parseResult.getLeads();
            List<String> columns = parseResult.getColumns();
            List<Map<String, String>> rawRowData = parseResult.getRawRowData();
            System.out.println("UPLOAD CSV - Parsed " + leads.size() + " leads");
            System.out.println("UPLOAD CSV - Detected " + columns.size() + " columns: " + columns);
            System.out.println("UPLOAD CSV - Stored " + rawRowData.size() + " raw row data maps");
            
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
            String csvFilename = file.getOriginalFilename();
            System.out.println("UPLOAD CSV - Creating campaign with name: " + name + " for user_id: " + userId + " with CSV: " + csvFilename);
            
            // STEP 2: Store CSV columns as JSON (these become {{variables}})
            String csvColumnsJson = objectMapper.writeValueAsString(columns);
            
            Campaign campaign = campaignService.createCampaign(userId, name, description, leads, csvFilename, csvColumnsJson);
            System.out.println("UPLOAD CSV - Campaign created with ID: " + campaign.id());
            
            // STEP 4: Import leads to database with full CSV row data as JSON
            System.out.println("UPLOAD CSV - Importing leads to database for user_id: " + userId);
            List<Map<String, Object>> importedLeads;
            try {
                importedLeads = leadImportService.importLeadsToDatabase(leads, rawRowData, userId);
                System.out.println("UPLOAD CSV - Imported " + importedLeads.size() + " leads to database");
            } catch (Exception e) {
                System.err.println("UPLOAD CSV - ERROR importing leads: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to import leads: " + e.getMessage(), e);
            }
            
            // Link leads to campaign
            Integer campaignId = Integer.parseInt(campaign.id());
            System.out.println("UPLOAD CSV - Linking leads to campaign_id: " + campaignId);
            try {
                campaignLeadService.linkLeadsToCampaign(campaignId, importedLeads);
                System.out.println("UPLOAD CSV - Successfully linked leads to campaign");
            } catch (Exception e) {
                System.err.println("UPLOAD CSV - ERROR linking leads to campaign: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to link leads to campaign: " + e.getMessage(), e);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("campaign", campaign);
            response.put("leadsCount", leads.size());
            response.put("importedLeads", importedLeads);
            response.put("columns", columns); // Add CSV columns to response
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("message", "CSV uploaded and campaign created successfully. " + importedLeads.size() + " leads imported to database.");
            
            System.out.println("UPLOAD CSV - Success! Returning response with " + columns.size() + " columns");
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
    
    /**
     * Get available merge fields (CSV columns) for a campaign
     */
    @GetMapping("/campaigns/{id}/merge-fields")
    public ResponseEntity<?> getMergeFields(@PathVariable String id) {
        try {
            Optional<Campaign> campaignOpt = campaignService.getCampaignById(id);
            if (campaignOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Campaign campaign = campaignOpt.get();
            List<String> columns = campaign.csvColumns() != null ? campaign.csvColumns() : new ArrayList<>();
            
            // Convert column names to variable format
            List<Map<String, String>> mergeFields = new ArrayList<>();
            for (String column : columns) {
                String variableKey = column.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
                Map<String, String> field = new HashMap<>();
                field.put("name", column);
                field.put("variable", variableKey);
                field.put("usage", "{{" + variableKey + "}}");
                mergeFields.add(field);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("columns", columns);
            response.put("mergeFields", mergeFields);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("GET MERGE FIELDS - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get merge fields: " + e.getMessage()));
        }
    }
    
    /**
     * Preview email with a sample lead from the campaign's CSV
     */
    @PostMapping("/campaigns/{id}/preview-email")
    public ResponseEntity<?> previewEmail(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> request,
            @RequestParam(value = "leadId", required = false) Integer leadId,
            @RequestParam(value = "spintaxSeed", required = false) String spintaxSeedStr) {
        try {
            System.out.println("PREVIEW EMAIL - Received request for campaign ID: " + id);
            System.out.println("PREVIEW EMAIL - Request body: " + (request != null ? request.toString() : "null"));
            System.out.println("PREVIEW EMAIL - leadId param: " + leadId);
            System.out.println("PREVIEW EMAIL - spintaxSeed param (raw): " + spintaxSeedStr);
            
            // Parse spintax seed - handle both integer and decimal strings gracefully
            Long spintaxSeed = null;
            if (spintaxSeedStr != null && !spintaxSeedStr.trim().isEmpty()) {
                try {
                    // Convert to double first to handle decimal strings, then floor to integer
                    double seedDouble = Double.parseDouble(spintaxSeedStr.trim());
                    spintaxSeed = (long) Math.floor(seedDouble);
                    System.out.println("PREVIEW EMAIL - Parsed spintaxSeed: " + spintaxSeed);
                } catch (NumberFormatException e) {
                    System.err.println("PREVIEW EMAIL - Invalid spintaxSeed format: " + spintaxSeedStr + ", using current time");
                    spintaxSeed = null; // Will fall back to current time
                }
            }
            
            if (request == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request body is required"));
            }
            
            Integer campaignId = Integer.parseInt(id);
            Optional<Campaign> campaignOpt = campaignService.getCampaignById(id);
            if (campaignOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Campaign not found"));
            }
            
            Campaign campaign = campaignOpt.get();
            String subjectTemplate = request.getOrDefault("subject", "");
            String bodyTemplate = request.getOrDefault("body", "");
            
            System.out.println("PREVIEW EMAIL - Subject template: " + subjectTemplate);
            System.out.println("PREVIEW EMAIL - Body template length: " + (bodyTemplate != null ? bodyTemplate.length() : 0));
            System.out.println("PREVIEW EMAIL - Body template preview: " + (bodyTemplate != null ? bodyTemplate.substring(0, Math.min(200, bodyTemplate.length())) : "null"));
            
            // Get a lead for preview - MUST have CSV data for variables to work
            LeadEntity lead;
            if (leadId != null) {
                lead = leadRepository.findById(leadId).orElse(null);
                System.out.println("PREVIEW EMAIL - Using specific lead ID: " + leadId);
                // If specified lead has no CSV data, find one that does
                if (lead != null && (lead.getCsvData() == null || lead.getCsvData().trim().isEmpty())) {
                    System.out.println("PREVIEW EMAIL - Specified lead has no CSV data, finding one with CSV data...");
                    LeadEntity csvLead = campaignLeadService.getLeadWithCsvData(campaignId);
                    if (csvLead != null) {
                        lead = csvLead;
                        System.out.println("PREVIEW EMAIL - Switched to lead with CSV data: " + lead.getId());
                    }
                }
            } else {
                // Always use random lead selection for preview (different lead each refresh)
                // This gives users variety when testing their email templates
                lead = campaignLeadService.getRandomLeadForCampaign(campaignId);
                if (lead != null) {
                    System.out.println("PREVIEW EMAIL - Using random lead for preview: " + lead.getId());
                } else {
                    System.out.println("PREVIEW EMAIL - No leads found for campaign " + campaignId);
                }
            }
            
            if (lead == null) {
                System.out.println("PREVIEW EMAIL - ERROR: No leads found for campaign " + campaignId);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No leads found for this campaign. Please upload a CSV file first."));
            }
            
            System.out.println("PREVIEW EMAIL - Lead data: firstName=" + lead.getFirstName() + ", email=" + lead.getEmail());
            System.out.println("PREVIEW EMAIL - Lead ID: " + lead.getId());
            System.out.println("PREVIEW EMAIL - Lead has CSV data: " + (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()));
            if (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()) {
                System.out.println("PREVIEW EMAIL - Lead CSV data length: " + lead.getCsvData().length());
                System.out.println("PREVIEW EMAIL - Lead CSV data preview: " + lead.getCsvData().substring(0, Math.min(500, lead.getCsvData().length())));
            } else {
                System.err.println("PREVIEW EMAIL - WARNING: Lead " + lead.getId() + " has NO CSV data! Variables will not be replaced.");
            }
            
            // Use provided spintax seed or generate one from current time for rotation
            // Each preview refresh will get a different seed, showing different spintax selections
            long seed = (spintaxSeed != null) ? spintaxSeed : System.currentTimeMillis();
            System.out.println("PREVIEW EMAIL - Using spintax seed: " + seed);
            
            // Render email with lead data and spintax expansion
            List<String> csvColumns = campaign.csvColumns() != null ? campaign.csvColumns() : new ArrayList<>();
            System.out.println("PREVIEW EMAIL - CSV columns from campaign: " + csvColumns);
            System.out.println("PREVIEW EMAIL - CSV columns count: " + csvColumns.size());
            
            String renderedSubject;
            String renderedBody;
            try {
                renderedSubject = emailRenderService.renderEmail(subjectTemplate, lead, csvColumns, seed);
                renderedBody = emailRenderService.renderEmail(bodyTemplate, lead, csvColumns, seed);
                
                System.out.println("PREVIEW EMAIL - Rendered subject: " + renderedSubject);
                if (renderedBody != null && renderedBody.length() > 0) {
                    System.out.println("PREVIEW EMAIL - Rendered body preview: " + renderedBody.substring(0, Math.min(200, renderedBody.length())));
                } else {
                    System.out.println("PREVIEW EMAIL - Rendered body is empty");
                }
            } catch (Exception renderException) {
                System.err.println("PREVIEW EMAIL - Error during email rendering: " + renderException.getMessage());
                renderException.printStackTrace();
                // Return template as-is if rendering fails
                renderedSubject = subjectTemplate;
                renderedBody = bodyTemplate;
            }
            
            // Parse CSV data to include in response for debugging
            Map<String, String> csvDataMap = null;
            if (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()) {
                try {
                    csvDataMap = objectMapper.readValue(lead.getCsvData(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    System.out.println("PREVIEW EMAIL - Parsed CSV data map with " + csvDataMap.size() + " fields");
                } catch (Exception e) {
                    System.err.println("PREVIEW EMAIL - Error parsing CSV data: " + e.getMessage());
                }
            }
            
            // Build response with lead info
            Map<String, Object> leadInfo = new HashMap<>();
            leadInfo.put("leadId", lead.getId());
            leadInfo.put("firstName", lead.getFirstName());
            leadInfo.put("lastName", lead.getLastName());
            leadInfo.put("companyName", lead.getCompanyName());
            leadInfo.put("email", lead.getEmail());
            leadInfo.put("jobTitle", lead.getJobTitle());
            // Include CSV data in response for debugging
            if (csvDataMap != null) {
                leadInfo.put("csvData", csvDataMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("subject", renderedSubject);
            response.put("body", renderedBody);
            response.put("lead", leadInfo);
            response.put("message", "Email preview rendered using sample lead data");
            
            // Include CSV columns and available variables for debugging
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("csvColumns", csvColumns);
            debugInfo.put("csvColumnsCount", csvColumns.size());
            if (csvDataMap != null) {
                debugInfo.put("csvDataKeys", new ArrayList<>(csvDataMap.keySet()));
                debugInfo.put("csvDataKeysCount", csvDataMap.size());
            } else {
                debugInfo.put("csvDataKeys", new ArrayList<>());
                debugInfo.put("csvDataKeysCount", 0);
                debugInfo.put("warning", "No CSV data found for this lead. Variables may not work correctly.");
            }
            response.put("debug", debugInfo);
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            System.err.println("PREVIEW EMAIL - Invalid campaign ID format: " + id);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid campaign ID: " + id));
        } catch (IllegalArgumentException e) {
            System.err.println("PREVIEW EMAIL - IllegalArgumentException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("PREVIEW EMAIL - Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to preview email: " + e.getMessage()));
        }
    }

    @PutMapping("/campaigns/{id}/email")
    public ResponseEntity<?> saveCampaignEmail(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            String emailSubject = request.get("emailSubject");
            String emailBody = request.get("emailBody");
            
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            campaignService.saveCampaignEmail(id, userId, emailSubject, emailBody);
            
            return ResponseEntity.ok(Map.of(
                "message", "Email saved successfully",
                "campaignId", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to save email: " + e.getMessage()));
        }
    }
}

