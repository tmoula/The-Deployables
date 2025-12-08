package com.outreach.campaign.application;

import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.infrastructure.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeadImportService {
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;
    
    public LeadImportService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Import leads to database, storing full CSV row data as JSON
     * @param leads List of Lead objects (for backward compatibility)
     * @param rawRowData List of Maps containing full CSV row data (column -> value)
     * @param userId User ID
     * @return List of imported lead info
     */
    @Transactional
    public List<Map<String, Object>> importLeadsToDatabase(
            List<Lead> leads, 
            List<Map<String, String>> rawRowData,
            Integer userId) {
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        
        System.out.println("Importing " + leads.size() + " leads to database for user_id: " + userId);
        
        // Use rawRowData if available, otherwise fall back to Lead objects
        boolean useRawData = rawRowData != null && !rawRowData.isEmpty() && rawRowData.size() == leads.size();
        
        for (int i = 0; i < leads.size(); i++) {
            Lead lead = leads.get(i);
            Map<String, String> rowData = useRawData ? rawRowData.get(i) : null;
            
            try {
                // Create LeadEntity from Lead record
                LeadEntity leadEntity = new LeadEntity();
                leadEntity.setUserId(userId);
                leadEntity.setBatchId(null); // Will be set when creating a lead batch
                // Truncate fields to prevent "value too long" errors (safety measure)
                leadEntity.setFirstName(truncate(lead.firstName(), 500));
                leadEntity.setLastName(truncate(lead.lastName(), 500));
                leadEntity.setJobTitle(truncate(lead.position(), 200));
                leadEntity.setCompanyName(truncate(lead.company(), 200));
                leadEntity.setCompanyWebsite(truncate(lead.domain(), 300));
                leadEntity.setCompanyLinkedin(null); // Not in CSV for now
                leadEntity.setLinkedinUrl(null); // Not in CSV for now
                leadEntity.setEmail(truncate(lead.email(), 250));
                leadEntity.setCountry(truncate(lead.regions(), 500)); // Using regions as country for now
                
                // Store additional CSV fields in custom_notes as JSON or comma-separated
                StringBuilder customNotes = new StringBuilder();
                if (lead.personalizationHook() != null) customNotes.append("Hook: ").append(lead.personalizationHook()).append("; ");
                if (lead.matchScore() != null) customNotes.append("Match: ").append(lead.matchScore()).append("; ");
                if (lead.industry() != null) customNotes.append("Industry: ").append(lead.industry()).append("; ");
                if (lead.companySize() != null) customNotes.append("Size: ").append(lead.companySize()).append("; ");
                if (lead.regions() != null) customNotes.append("Regions: ").append(lead.regions()).append("; ");
                if (lead.techStack() != null) customNotes.append("Tech: ").append(lead.techStack()).append("; ");
                if (lead.keywords() != null) customNotes.append("Keywords: ").append(lead.keywords()).append("; ");
                if (lead.notes() != null) customNotes.append("Notes: ").append(lead.notes());
                leadEntity.setCustomNotes(customNotes.length() > 0 ? customNotes.toString() : null);
                
                // STEP 4: Store full CSV row data as JSON (like Instantly does)
                // This allows us to use ANY CSV column as {{variable}}
                if (rowData != null) {
                    try {
                        String csvDataJson = objectMapper.writeValueAsString(rowData);
                        leadEntity.setCsvData(csvDataJson);
                        System.out.println("Stored CSV data for lead " + i + ": " + csvDataJson.substring(0, Math.min(100, csvDataJson.length())) + "...");
                    } catch (Exception e) {
                        System.err.println("Error serializing CSV data to JSON for lead " + i + ": " + e.getMessage());
                        e.printStackTrace();
                        // Continue without CSV data rather than failing the entire import
                    }
                }
                
                leadEntity.setCreatedAt(LocalDateTime.now());
                
                // Save to database - let database exceptions propagate
                System.out.println("Saving lead " + i + " to database...");
                leadEntity = leadRepository.save(leadEntity);
                System.out.println("Successfully saved lead " + i + " with ID: " + leadEntity.getId());
                
                // Return mapping info
                Map<String, Object> leadInfo = new HashMap<>();
                leadInfo.put("leadId", leadEntity.getId());
                leadInfo.put("userId", userId);
                leadInfo.put("firstName", leadEntity.getFirstName());
                leadInfo.put("lastName", leadEntity.getLastName());
                leadInfo.put("companyName", leadEntity.getCompanyName());
                leadInfo.put("email", leadEntity.getEmail());
                importedLeads.add(leadInfo);
                
                System.out.println("Imported lead: " + leadEntity.getFirstName() + " " + leadEntity.getLastName() + 
                                 " at " + leadEntity.getCompanyName() + " (ID: " + leadEntity.getId() + ")");
                
            } catch (org.springframework.dao.DataAccessException e) {
                // Database errors should propagate to trigger proper rollback
                System.err.println("Database error importing lead " + i + " (" + (lead.company() != null ? lead.company() : "unknown") + "): " + e.getMessage());
                System.err.println("Lead data: firstName=" + lead.firstName() + ", lastName=" + lead.lastName() + ", email=" + lead.email());
                e.printStackTrace();
                throw new RuntimeException("Database error while importing lead: " + e.getMessage(), e);
            } catch (Exception e) {
                // Other errors (parsing, etc.) - log but continue with other leads
                System.err.println("Error importing lead " + i + " (" + (lead.company() != null ? lead.company() : "unknown") + "): " + e.getMessage());
                e.printStackTrace();
                // Don't rethrow - continue processing other leads
            }
        }
        
        System.out.println("Successfully imported " + importedLeads.size() + " leads to database");
        return importedLeads;
    }
    
    /**
     * Truncate string to max length, returning null if input is null
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}

