package com.outreach.campaign.application;

import com.outreach.campaign.domain.Campaign;
import com.outreach.campaign.domain.Lead;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CampaignService {
    private final List<Campaign> campaigns = new ArrayList<>();
    
    public Campaign createCampaign(String name, String description, List<Lead> leads) {
        Campaign campaign = new Campaign(
            UUID.randomUUID().toString(),
            name,
            description,
            Campaign.CampaignStatus.DRAFT,
            LocalDateTime.now(),
            LocalDateTime.now(),
            leads != null ? new ArrayList<>(leads) : new ArrayList<>(),
            leads != null ? leads.size() : 0,
            0,
            0,
            0
        );
        campaigns.add(campaign);
        return campaign;
    }
    
    public List<Campaign> getAllCampaigns() {
        return new ArrayList<>(campaigns);
    }
    
    public Optional<Campaign> getCampaignById(String id) {
        return campaigns.stream()
            .filter(c -> c.id().equals(id))
            .findFirst();
    }
    
    public Campaign updateCampaignStatus(String id, Campaign.CampaignStatus status) {
        Campaign campaign = getCampaignById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
        
        Campaign updated = new Campaign(
            campaign.id(),
            campaign.name(),
            campaign.description(),
            status,
            campaign.createdAt(),
            LocalDateTime.now(),
            campaign.leads(),
            campaign.totalLeads(),
            campaign.sentCount(),
            campaign.openedCount(),
            campaign.repliedCount()
        );
        
        campaigns.remove(campaign);
        campaigns.add(updated);
        return updated;
    }
    
    public List<Lead> getCampaignLeads(String campaignId) {
        return getCampaignById(campaignId)
            .map(Campaign::leads)
            .orElse(List.of());
    }
}

