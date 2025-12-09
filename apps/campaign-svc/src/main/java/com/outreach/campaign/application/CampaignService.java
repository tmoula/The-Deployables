package com.outreach.campaign.application;

import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.infrastructure.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignLeadService campaignLeadService;
    private final ObjectMapper objectMapper;

    public CampaignService(CampaignRepository campaignRepository, CampaignLeadService campaignLeadService) {
        this.campaignRepository = campaignRepository;
        this.campaignLeadService = campaignLeadService;
        this.objectMapper = new ObjectMapper();
    }
    
    private List<String> parseCsvColumns(String csvColumnsJson) {
        if (csvColumnsJson == null || csvColumnsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(csvColumnsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing CSV columns JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Campaign createCampaign(Integer userId, String name, String description, List<Lead> leads, String csvFilename, String csvColumnsJson) {
        // Check for existing campaign with same name to prevent duplicates
        Optional<CampaignEntity> existing = campaignRepository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            System.out.println("Campaign with name '" + name + "' already exists for user_id: " + userId + ", returning existing campaign");
            CampaignEntity existingEntity = existing.get();
            // Update CSV info if provided
            if (csvFilename != null) {
                existingEntity.setCsvFilename(csvFilename);
            }
            if (csvColumnsJson != null) {
                existingEntity.setCsvColumns(csvColumnsJson);
            }
            existingEntity = campaignRepository.save(existingEntity);
            List<String> csvColumns = parseCsvColumns(existingEntity.getCsvColumns());
            long leadCount = campaignLeadService.getCampaignLeadCount(existingEntity.getId());
            return new Campaign(
                    String.valueOf(existingEntity.getId()),
                    existingEntity.getName(),
                    description,
                    Campaign.CampaignStatus.valueOf(existingEntity.getStatus().toUpperCase()),
                    existingEntity.getCreatedAt(),
                    LocalDateTime.now(),
                    new ArrayList<>(),
                    (int) leadCount,
                    0, 0, 0,
                    existingEntity.getCsvFilename(),
                    csvColumns,
                    existingEntity.getEmailSubject(),
                    existingEntity.getEmailBody()
            );
        }
        
        CampaignEntity entity = new CampaignEntity();
        entity.setName(name);
        entity.setStatus("draft");
        entity.setUserId(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCsvFilename(csvFilename);
        entity.setCsvColumns(csvColumnsJson);
        System.out.println("Creating campaign '" + name + "' for user_id: " + userId + (csvFilename != null ? " with CSV: " + csvFilename : ""));
        entity = campaignRepository.save(entity);
        System.out.println("Campaign created with id: " + entity.getId() + ", user_id: " + entity.getUserId());

        List<String> csvColumns = parseCsvColumns(csvColumnsJson);
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
                0,
                csvFilename,
                csvColumns,
                null,
                null
        );
    }

    public List<Campaign> getAllCampaigns(Integer userId) {
        System.out.println("Fetching campaigns for user_id: " + userId);
        List<CampaignEntity> entities = campaignRepository.findByUserId(userId);
        System.out.println("Found " + entities.size() + " campaigns for user_id: " + userId);

        return entities.stream()
                .map(entity -> {
                    // Get lead count from persisted campaign_leads
                    long leadCount = campaignLeadService.getCampaignLeadCount(entity.getId());
                    List<String> csvColumns = parseCsvColumns(entity.getCsvColumns());
                    return new Campaign(
                            String.valueOf(entity.getId()),
                            entity.getName(),
                            "",
                            Campaign.CampaignStatus.valueOf(entity.getStatus().toUpperCase()),
                            entity.getCreatedAt(),
                            entity.getCreatedAt(),
                            new ArrayList<>(),
                            (int) leadCount,
                            0,
                            0,
                            0,
                            entity.getCsvFilename(),
                            csvColumns,
                            entity.getEmailSubject(),
                            entity.getEmailBody()
                    );
                })
                .collect(Collectors.toList());
    }
    
    public Optional<Campaign> getCampaignById(String id) {
        try {
            Integer campaignId = Integer.parseInt(id);
            Optional<CampaignEntity> entityOpt = campaignRepository.findById(campaignId);
            if (entityOpt.isPresent()) {
                CampaignEntity e = entityOpt.get();
                long leadCount = campaignLeadService.getCampaignLeadCount(e.getId());
                List<String> csvColumns = parseCsvColumns(e.getCsvColumns());
                return Optional.of(new Campaign(
                        String.valueOf(e.getId()),
                        e.getName(),
                        "",
                        Campaign.CampaignStatus.valueOf(e.getStatus().toUpperCase()),
                        e.getCreatedAt(),
                        e.getCreatedAt(),
                        new ArrayList<>(),
                        (int) leadCount,
                        0,
                        0,
                        0,
                        e.getCsvFilename(),
                        csvColumns,
                        e.getEmailSubject(),
                        e.getEmailBody()
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
        
        long leadCount = campaignLeadService.getCampaignLeadCount(entity.getId());
        List<String> csvColumns = parseCsvColumns(entity.getCsvColumns());

        return new Campaign(
                String.valueOf(entity.getId()),
                entity.getName(),
                "",
            status,
                entity.getCreatedAt(),
            LocalDateTime.now(),
                new ArrayList<>(),
                (int) leadCount,
                0,
                0,
                0,
                entity.getCsvFilename(),
                csvColumns,
                entity.getEmailSubject(),
                entity.getEmailBody()
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

    public void saveCampaignEmail(Integer campaignId, Integer userId, String emailSubject, String emailBody) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        campaign.setEmailSubject(emailSubject);
        campaign.setEmailBody(emailBody);
        campaignRepository.save(campaign);
    }
}

