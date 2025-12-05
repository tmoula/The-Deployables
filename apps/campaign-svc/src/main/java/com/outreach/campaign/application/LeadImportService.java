package com.outreach.campaign.application;

import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.infrastructure.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeadImportService {
    private final LeadRepository leadRepository;
    
    public LeadImportService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }
    
    @Transactional
    public List<Map<String, Object>> importLeadsToDatabase(List<Lead> leads, Integer userId) {
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        
        System.out.println("Importing " + leads.size() + " leads to database for user_id: " + userId);
        
        for (Lead lead : leads) {
            try {
                // Create LeadEntity from Lead record
                LeadEntity leadEntity = new LeadEntity();
                leadEntity.setUserId(userId);
                leadEntity.setBatchId(null); // Will be set when creating a lead batch
                leadEntity.setFirstName(lead.firstName());
                leadEntity.setLastName(lead.lastName());
                leadEntity.setJobTitle(lead.position());
                leadEntity.setCompanyName(lead.company());
                leadEntity.setCompanyWebsite(lead.domain());
                leadEntity.setCompanyLinkedin(null); // Not in CSV for now
                leadEntity.setLinkedinUrl(null); // Not in CSV for now
                leadEntity.setEmail(lead.email());
                leadEntity.setCountry(lead.regions()); // Using regions as country for now
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
                leadEntity.setCreatedAt(LocalDateTime.now());
                
                // Save to database
                leadEntity = leadRepository.save(leadEntity);
                
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
                
            } catch (Exception e) {
                System.err.println("Error importing lead " + (lead.company() != null ? lead.company() : "unknown") + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Successfully imported " + importedLeads.size() + " leads to database");
        return importedLeads;
    }
}

