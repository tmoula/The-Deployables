// Exposes REST endpoints (/api/v1/...) so the frontend can talk to the backend
package com.outreach.lead.api;

import com.outreach.lead.application.MatchService;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.io.StringWriter;

@RestController
@RequestMapping("/api/v1")
public class MatchController {
    private final MatchService svc;
    public MatchController(MatchService svc){ this.svc = svc; }
    @GetMapping
    public Map<String, String> root() {
        return Map.of("status", "ok");
    }
    @PutMapping("/seller")
    public SellerProfile putSeller(@RequestBody SellerProfile s){ return svc.setSeller(s); }

    @GetMapping("/prospects") public List<Prospect> listProspects(){ return svc.listProspects(); }

    @PostMapping("/prospects")
    public Prospect createProspect(@RequestBody Prospect prospect) {
        return svc.createProspect(prospect);
    }

    @PostMapping("/match")
    public List<MatchService.ScoredProspect> match(
            @RequestBody(required=false) ProspectCriteria criteria,
            @RequestParam(defaultValue="20") int limit) {
        try {
            System.out.println("=== MATCH CONTROLLER: Received match request ===");
            System.out.println("Criteria: " + (criteria != null ? criteria.toString() : "null"));
            System.out.println("Limit: " + limit);
            
            if (criteria == null) {
                criteria = new ProspectCriteria(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            }
            
            List<MatchService.ScoredProspect> results = svc.match(criteria, limit);
            System.out.println("=== MATCH CONTROLLER: Returning " + results.size() + " results ===");
            return results;
        } catch (Exception e) {
            System.err.println("=== MATCH CONTROLLER ERROR ===");
            System.err.println("Error in match endpoint: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Return empty list on error
        }
    }

    @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","ok"); }

    /**
     * Generate CSV from matched prospects
     * User provides criteria, system calls Gemini AI, returns CSV
     */
    @PostMapping("/generate-csv")
    public ResponseEntity<String> generateCsv(
            @RequestBody(required=false) ProspectCriteria criteria,
            @RequestParam(defaultValue="20") int limit) {
        
        if (criteria == null) {
            criteria = new ProspectCriteria(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        
        // Get matched prospects (this will call Gemini AI dynamically)
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
