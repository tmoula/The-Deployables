package com.outreach.campaign.application;

import com.outreach.campaign.domain.entities.CampaignLeadEntity;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.infrastructure.CampaignLeadRepository;
import com.outreach.campaign.infrastructure.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CampaignLeadService {
    private final CampaignLeadRepository campaignLeadRepository;
    private final LeadRepository leadRepository;
    
    public CampaignLeadService(
        CampaignLeadRepository campaignLeadRepository,
        LeadRepository leadRepository
    ) {
        this.campaignLeadRepository = campaignLeadRepository;
        this.leadRepository = leadRepository;
    }
    
    /**
     * Link imported leads to a campaign
     * @param campaignId The campaign ID
     * @param importedLeads List of imported lead info (from LeadImportService)
     */
    @Transactional
    public void linkLeadsToCampaign(Integer campaignId, List<Map<String, Object>> importedLeads) {
        System.out.println("Linking " + importedLeads.size() + " leads to campaign_id: " + campaignId);
        
        // First, remove any existing campaign leads (in case of re-upload)
        List<CampaignLeadEntity> existing = campaignLeadRepository.findByCampaignId(campaignId);
        if (!existing.isEmpty()) {
            System.out.println("Removing " + existing.size() + " existing campaign leads");
            campaignLeadRepository.deleteAll(existing);
        }
        
        // Link each imported lead to the campaign
        int linkedCount = 0;
        for (Map<String, Object> leadInfo : importedLeads) {
            Integer leadId = (Integer) leadInfo.get("leadId");
            if (leadId != null) {
                try {
                    CampaignLeadEntity campaignLead = new CampaignLeadEntity();
                    campaignLead.setCampaignId(campaignId);
                    campaignLead.setLeadId(leadId);
                    campaignLead.setStatus("queued");
                    campaignLead.setCreatedAt(LocalDateTime.now());
                    campaignLeadRepository.save(campaignLead);
                    linkedCount++;
                    System.out.println("Linked lead_id: " + leadId + " to campaign_id: " + campaignId);
                } catch (Exception e) {
                    System.err.println("Error linking lead_id: " + leadId + " to campaign_id: " + campaignId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Warning: leadInfo missing leadId: " + leadInfo);
            }
        }
        
        System.out.println("Successfully linked " + linkedCount + " out of " + importedLeads.size() + " leads to campaign_id: " + campaignId);
    }
    
    /**
     * Get a random lead from a campaign for preview purposes
     * Prefers leads with CSV data (from CSV uploads) over AI-generated leads
     * @param campaignId The campaign ID
     * @return LeadEntity or null if no leads found
     */
    public LeadEntity getRandomLeadForCampaign(Integer campaignId) {
        List<CampaignLeadEntity> campaignLeads = campaignLeadRepository.findByCampaignId(campaignId);
        if (campaignLeads.isEmpty()) {
            return null;
        }
        
        // First, try to find a lead with CSV data (from CSV uploads)
        List<LeadEntity> leadsWithCsvData = new ArrayList<>();
        for (CampaignLeadEntity campaignLead : campaignLeads) {
            LeadEntity lead = leadRepository.findById(campaignLead.getLeadId()).orElse(null);
            if (lead != null && lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()) {
                leadsWithCsvData.add(lead);
            }
        }
        
        // If we found leads with CSV data, use one of those
        if (!leadsWithCsvData.isEmpty()) {
            int randomIndex = (int) (Math.random() * leadsWithCsvData.size());
            System.out.println("getRandomLeadForCampaign - Using lead with CSV data: " + leadsWithCsvData.get(randomIndex).getId());
            return leadsWithCsvData.get(randomIndex);
        }
        
        // Otherwise, fall back to any lead
        int randomIndex = (int) (Math.random() * campaignLeads.size());
        CampaignLeadEntity campaignLead = campaignLeads.get(randomIndex);
        LeadEntity lead = leadRepository.findById(campaignLead.getLeadId()).orElse(null);
        if (lead != null) {
            System.out.println("getRandomLeadForCampaign - Using lead without CSV data (fallback): " + lead.getId());
        }
        return lead;
    }
    
    /**
     * Get all leads for a campaign
     * @param campaignId The campaign ID
     * @return List of LeadEntity
     */
    public List<LeadEntity> getCampaignLeads(Integer campaignId) {
        List<CampaignLeadEntity> campaignLeads = campaignLeadRepository.findByCampaignId(campaignId);
        return campaignLeads.stream()
            .map(cl -> leadRepository.findById(cl.getLeadId()).orElse(null))
            .filter(lead -> lead != null)
            .toList();
    }
    
    /**
     * Get lead count for a campaign (only counts leads with emails)
     * @param campaignId The campaign ID
     * @return Number of leads with valid emails
     */
    public long getCampaignLeadCount(Integer campaignId) {
        List<CampaignLeadEntity> campaignLeads = campaignLeadRepository.findByCampaignId(campaignId);
        // Count only leads that have email addresses
        return campaignLeads.stream()
            .map(cl -> leadRepository.findById(cl.getLeadId()).orElse(null))
            .filter(lead -> lead != null && lead.getEmail() != null && !lead.getEmail().trim().isEmpty())
            .count();
    }
}

