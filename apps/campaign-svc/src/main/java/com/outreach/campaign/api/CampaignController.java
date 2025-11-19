package com.outreach.campaign.api;

import com.outreach.campaign.application.CampaignService;
import com.outreach.campaign.application.CsvParserService;
import com.outreach.campaign.domain.Campaign;
import com.outreach.campaign.domain.Lead;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CampaignController {
    private final CampaignService campaignService;
    private final CsvParserService csvParserService;
    
    public CampaignController(CampaignService campaignService, CsvParserService csvParserService) {
        this.campaignService = campaignService;
        this.csvParserService = csvParserService;
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
            
            // Create campaign
            String name = campaignName.trim();
            
            Campaign campaign = campaignService.createCampaign(name, description, leads);
            
            Map<String, Object> response = new HashMap<>();
            response.put("campaign", campaign);
            response.put("leadsCount", leads.size());
            response.put("message", "CSV uploaded and campaign created successfully");
            
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

