package com.outreach.campaign.application;

import com.outreach.campaign.domain.Campaign;
import com.outreach.campaign.domain.CampaignEntity;
import com.outreach.campaign.domain.Lead;
import com.outreach.campaign.infrastructure.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final List<Campaign> inMemoryCampaigns = new ArrayList<>(); // Keep for backward compatibility
    
    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }
    
    @Transactional
    public Campaign createCampaign(String name, String description, List<Lead> leads) {
        // Save to database
        CampaignEntity entity = new CampaignEntity();
        entity.setCampaignName(name);
        entity.setStatus("draft");
        entity.setUserId(1); // Default user ID for now
        entity.setCreatedAt(LocalDateTime.now());
        entity = campaignRepository.save(entity);
        
        // Also create in-memory version for backward compatibility
        Campaign campaign = new Campaign(
            String.valueOf(entity.getCampaignId()),
            name,
            description,
            Campaign.CampaignStatus.DRAFT,
            entity.getCreatedAt(),
            LocalDateTime.now(),
            leads != null ? new ArrayList<>(leads) : new ArrayList<>(),
            leads != null ? leads.size() : 0,
            0,
            0,
            0
        );
        inMemoryCampaigns.add(campaign);
        return campaign;
    }
    
    public List<Campaign> getAllCampaigns() {
        // Load from database and convert to domain objects
        List<CampaignEntity> entities = campaignRepository.findAll();
        List<Campaign> campaigns = entities.stream()
            .map(entity -> new Campaign(
                String.valueOf(entity.getCampaignId()),
                entity.getCampaignName(),
                "", // description not in DB schema yet
                Campaign.CampaignStatus.valueOf(entity.getStatus().toUpperCase()),
                entity.getCreatedAt(),
                entity.getCreatedAt(),
                new ArrayList<>(),
                0,
                0,
                0,
                0
            ))
            .collect(Collectors.toList());
        
        // Merge with in-memory campaigns (for campaigns created before DB migration)
        campaigns.addAll(inMemoryCampaigns);
        return campaigns;
    }
    
    public Optional<Campaign> getCampaignById(String id) {
        try {
            Integer campaignId = Integer.parseInt(id);
            Optional<CampaignEntity> entity = campaignRepository.findById(campaignId);
            if (entity.isPresent()) {
                CampaignEntity e = entity.get();
                Campaign campaign = new Campaign(
                    String.valueOf(e.getCampaignId()),
                    e.getCampaignName(),
                    "",
                    Campaign.CampaignStatus.valueOf(e.getStatus().toUpperCase()),
                    e.getCreatedAt(),
                    e.getCreatedAt(),
                    new ArrayList<>(),
                    0,
                    0,
                    0,
                    0
                );
                return Optional.of(campaign);
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, try in-memory
        }
        
        // Fallback to in-memory
        return inMemoryCampaigns.stream()
            .filter(c -> c.id().equals(id))
            .findFirst();
    }
    
    @Transactional
    public Campaign updateCampaignStatus(String id, Campaign.CampaignStatus status) {
        try {
            Integer campaignId = Integer.parseInt(id);
            Optional<CampaignEntity> entityOpt = campaignRepository.findById(campaignId);
            if (entityOpt.isPresent()) {
                CampaignEntity entity = entityOpt.get();
                entity.setStatus(status.name().toLowerCase());
                campaignRepository.save(entity);
                
                Campaign campaign = new Campaign(
                    String.valueOf(entity.getCampaignId()),
                    entity.getCampaignName(),
                    "",
                    status,
                    entity.getCreatedAt(),
                    LocalDateTime.now(),
                    new ArrayList<>(),
                    0,
                    0,
                    0,
                    0
                );
                return campaign;
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, try in-memory
        }
        
        // Fallback to in-memory
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
        
        inMemoryCampaigns.remove(campaign);
        inMemoryCampaigns.add(updated);
        return updated;
    }
    
    public List<Lead> getCampaignLeads(String campaignId) {
        return getCampaignById(campaignId)
            .map(Campaign::leads)
            .orElse(List.of());
    }
}

