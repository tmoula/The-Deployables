package com.outreach.campaign.application;

import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.infrastructure.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public Campaign createCampaign(Integer userId, String name, String description, List<Lead> leads) {
        CampaignEntity entity = new CampaignEntity();
        entity.setName(name);
        entity.setStatus("draft");
        entity.setUserId(userId);
        entity.setCreatedAt(LocalDateTime.now());
        System.out.println("Creating campaign '" + name + "' for user_id: " + userId);
        entity = campaignRepository.save(entity);
        System.out.println("Campaign created with id: " + entity.getId() + ", user_id: " + entity.getUserId());

        return new Campaign(
                String.valueOf(entity.getId()),
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
    }

    public List<Campaign> getAllCampaigns(Integer userId) {
        System.out.println("Fetching campaigns for user_id: " + userId);
        List<CampaignEntity> entities = campaignRepository.findByUserId(userId);
        System.out.println("Found " + entities.size() + " campaigns for user_id: " + userId);

        return entities.stream()
                .map(entity -> new Campaign(
                        String.valueOf(entity.getId()),
                        entity.getName(),
                        "",
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
    }

    public Optional<Campaign> getCampaignById(String id) {
        try {
            Integer campaignId = Integer.parseInt(id);
            Optional<CampaignEntity> entityOpt = campaignRepository.findById(campaignId);
            if (entityOpt.isPresent()) {
                CampaignEntity e = entityOpt.get();
                return Optional.of(new Campaign(
                        String.valueOf(e.getId()),
                        e.getName(),
                        "",
                        Campaign.CampaignStatus.valueOf(e.getStatus().toUpperCase()),
                        e.getCreatedAt(),
                        e.getCreatedAt(),
                        new ArrayList<>(),
                        0,
                        0,
                        0,
                        0
                ));
            }
        } catch (NumberFormatException ignored) {
        }

        return Optional.empty();
    }

    @Transactional
    public Campaign updateCampaignStatus(String id, Campaign.CampaignStatus status) {
        Integer campaignId;
        try {
            campaignId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid campaign id: " + id);
        }

        CampaignEntity entity = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        entity.setStatus(status.name().toLowerCase());
        campaignRepository.save(entity);

        return new Campaign(
                String.valueOf(entity.getId()),
                entity.getName(),
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
    }

    public List<Lead> getCampaignLeads(String campaignId) {
        return List.of();
    }

    /**
     * Delete a campaign. Verifies ownership before deletion.
     */
    @Transactional
    public void deleteCampaign(String id, Integer userId) {
        Integer campaignId;
        try {
            campaignId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid campaign id: " + id);
        }

        System.out.println("Attempting to delete campaign_id: " + campaignId + " by user_id: " + userId);
        CampaignEntity entity = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        System.out.println("Campaign found - id: " + entity.getId() + ", owner user_id: " + entity.getUserId() + ", requesting user_id: " + userId);

        // Verify ownership - user can only delete their own campaigns
        if (!userId.equals(entity.getUserId())) {
            System.out.println("DELETE REJECTED: User " + userId + " cannot delete campaign owned by user " + entity.getUserId());
            throw new IllegalArgumentException("You are not allowed to delete this campaign");
        }

        System.out.println("DELETE APPROVED: Deleting campaign_id: " + campaignId);
        campaignRepository.delete(entity);
        System.out.println("Campaign deleted successfully");
    }
}

