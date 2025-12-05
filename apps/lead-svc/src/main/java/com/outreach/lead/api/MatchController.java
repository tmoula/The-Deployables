// Exposes REST endpoints (/api/v1/...) so the frontend can talk to the backend
package com.outreach.lead.api;

import com.outreach.lead.api.DTO.LeadBatchResponse;
import com.outreach.lead.api.DTO.LeadBatchStatusResponse;
import com.outreach.lead.application.MatchService;
import com.outreach.lead.application.UserContextService;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import com.outreach.lead.domain.entities.LeadBatchEntity;
import com.outreach.lead.domain.entities.LeadEntity;
import com.outreach.lead.infrastructure.LeadBatchRepository;
import com.outreach.lead.infrastructure.LeadRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MatchController {
    private final MatchService svc;
    private final UserContextService userContextService;
    private final LeadBatchRepository leadBatchRepository;
    private final LeadRepository leadRepository;
    
    public MatchController(
        MatchService svc,
        UserContextService userContextService,
        LeadBatchRepository leadBatchRepository,
        LeadRepository leadRepository
    ) {
        this.svc = svc;
        this.userContextService = userContextService;
        this.leadBatchRepository = leadBatchRepository;
        this.leadRepository = leadRepository;
    }
    @GetMapping
    public Map<String, String> root() {
        return Map.of("status", "ok");
    }
    @PutMapping("/seller")
    public SellerProfile putSeller(@RequestBody SellerProfile s){ return svc.setSeller(s); }
    
    @GetMapping("/seller")
    public ResponseEntity<SellerProfile> getSeller() {
        SellerProfile seller = svc.getSeller();
        if (seller == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(seller);
    }

    @GetMapping("/prospects") public List<Prospect> listProspects(){ return svc.listProspects(); }

    @PostMapping("/prospects")
    public Prospect createProspect(@RequestBody Prospect prospect) {
        return svc.createProspect(prospect);
    }

    @PostMapping("/match")
    public ResponseEntity<LeadBatchResponse> match(
            @RequestBody(required=false) ProspectCriteria criteria,
            @RequestParam(defaultValue="5") int limit,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            System.out.println("=== MATCH CONTROLLER: Received match request ===");
            System.out.println("User Email: " + userEmail);
            System.out.println("Criteria: " + (criteria != null ? criteria.toString() : "null"));
            System.out.println("Limit: " + limit);
            
            // Get user_id from email
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            System.out.println("Resolved user_id: " + userId);
            
            // Get seller profile (should be set via PUT /seller)
            SellerProfile seller = svc.getSeller();
            if (seller == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller profile not set. Please set seller profile first.");
            }
            
            if (criteria == null) {
                criteria = new ProspectCriteria(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            }
            
            // Start lead generation (saves to DB, publishes to RabbitMQ)
            LeadBatchResponse response = svc.startLeadGeneration(seller, criteria, limit, userId);
            System.out.println("=== MATCH CONTROLLER: Returning batch_id: " + response.batchId() + " ===");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("=== MATCH CONTROLLER ERROR: " + e.getMessage() + " ===");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            System.err.println("=== MATCH CONTROLLER ERROR ===");
            System.err.println("Error in match endpoint: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to start lead generation: " + e.getMessage());
        }
    }
    
    /**
     * Get batch status
     */
    @GetMapping("/lead-batches/{batchId}")
    public ResponseEntity<LeadBatchStatusResponse> getBatchStatus(
            @PathVariable Integer batchId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            
            LeadBatchEntity batch = leadBatchRepository.findByIdAndUserId(batchId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
            
            LeadBatchStatusResponse response = new LeadBatchStatusResponse(
                batch.getId(),
                batch.getUserId(),
                batch.getIcpId(),
                batch.getSource(),
                batch.getStatus(),
                batch.getRequestedLeadCount(),
                batch.getTotalLeads(),
                batch.getErrorMessage()
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * Get leads for a batch
     */
    @GetMapping("/lead-batches/{batchId}/leads")
    public ResponseEntity<List<Map<String, Object>>> getBatchLeads(
            @PathVariable Integer batchId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            Integer userId = userContextService.getUserIdFromEmail(userEmail);
            
            // Verify batch belongs to user
            LeadBatchEntity batch = leadBatchRepository.findByIdAndUserId(batchId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
            
            // Get leads for this batch
            List<LeadEntity> leads = leadRepository.findByUserIdAndBatchId(userId, batchId);
            
            // Convert to DTO
            List<Map<String, Object>> leadDTOs = leads.stream().map(lead -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", lead.getId());
                dto.put("firstName", lead.getFirstName());
                dto.put("lastName", lead.getLastName());
                dto.put("jobTitle", lead.getJobTitle());
                dto.put("companyName", lead.getCompanyName());
                dto.put("companyWebsite", lead.getCompanyWebsite());
                dto.put("email", lead.getEmail());
                dto.put("country", lead.getCountry());
                return dto;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(leadDTOs);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","ok"); }

    /**
     * Generate CSV from matched prospects
     * User provides criteria, system calls AI Service, returns CSV
     */
    @PostMapping("/generate-csv")
    public ResponseEntity<String> generateCsv(
            @RequestBody(required=false) ProspectCriteria criteria,
            @RequestParam(defaultValue="20") int limit) {
        
        if (criteria == null) {
            criteria = new ProspectCriteria(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        
        // Get matched prospects (this will call AI Service dynamically)
        List<MatchService.ScoredProspect> scoredProspects = svc.match(criteria, limit);
        
        // Generate CSV
        String csv = generateCsvFromProspects(scoredProspects);
        
        // Return CSV as downloadable file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "prospects.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    /**
     * Convert prospects to CSV format
     */
    private String generateCsvFromProspects(List<MatchService.ScoredProspect> scoredProspects) {
        StringWriter writer = new StringWriter();
        
        // CSV Header
        writer.append("ID,Company,First Name,Last Name,Position,Email,Domain,Industry,Size,Regions,Score,Personalization Hook\n");
        
        // CSV Rows
        for (MatchService.ScoredProspect scored : scoredProspects) {
            Prospect p = scored.prospect();
            writer.append(escapeCsv(p.id())).append(",");
            writer.append(escapeCsv(p.company())).append(",");
            writer.append(escapeCsv(p.firstName())).append(",");
            writer.append(escapeCsv(p.lastName())).append(",");
            writer.append(escapeCsv(p.position())).append(",");
            writer.append(escapeCsv(p.email())).append(",");
            writer.append(escapeCsv(p.domain())).append(",");
            writer.append(escapeCsv(p.industry())).append(",");
            writer.append(p.size() != null ? p.size().toString() : "").append(",");
            writer.append(escapeCsv(p.regions() != null ? String.join(";", p.regions()) : "")).append(",");
            writer.append(String.format("%.2f", scored.score())).append(",");
            writer.append(escapeCsv(p.personalizationHook() != null ? p.personalizationHook() : "")).append("\n");
        }
        
        return writer.toString();
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
