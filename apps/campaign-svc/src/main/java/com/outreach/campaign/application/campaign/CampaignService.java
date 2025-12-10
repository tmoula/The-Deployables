package com.outreach.campaign.application.campaign;

import com.outreach.campaign.application.lead.CampaignLeadService;
import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.domain.entities.CampaignMailboxEntity;
import com.outreach.campaign.domain.models.Lead;
import com.outreach.campaign.infrastructure.CampaignRepository;
import com.outreach.campaign.infrastructure.CampaignMailboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignLeadService campaignLeadService;
    private final CampaignMailboxRepository campaignMailboxRepository;
    private final ObjectMapper objectMapper;

    public CampaignService(
        CampaignRepository campaignRepository, 
        CampaignLeadService campaignLeadService,
        CampaignMailboxRepository campaignMailboxRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignLeadService = campaignLeadService;
        this.campaignMailboxRepository = campaignMailboxRepository;
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
    
    /**
     * Schedule a campaign with start date and selected mailboxes
     */
    @Transactional
    public void scheduleCampaign(Integer campaignId, Integer userId, Map<String, Object> settings) {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        if (!campaign.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Parse start date (expected to be in ISO format with UTC timezone)
        if (settings.containsKey("startDate") && settings.get("startDate") != null) {
            String startDateStr = settings.get("startDate").toString();
            if (!startDateStr.trim().isEmpty()) {
                try {
                    // Parse ISO format with timezone (e.g., "2024-01-15T14:00:00.000Z" or "2024-01-15T14:00:00Z")
                    // Frontend sends UTC time, so we parse it and store as LocalDateTime (treating it as UTC)
                    LocalDateTime startAt = null;
                    
                    // Parse ISO 8601 format with timezone
                    if (startDateStr.contains("T")) {
                        // Parse as ZonedDateTime to handle timezone, then convert to UTC LocalDateTime
                        try {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(startDateStr);
                            // Convert to UTC LocalDateTime for storage
                            startAt = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
                            System.out.println("SCHEDULE CAMPAIGN - Parsed ISO date: " + startDateStr + " -> UTC: " + startAt);
                        } catch (Exception e1) {
                            // Try parsing as Instant (ends with Z)
                            try {
                                java.time.Instant instant = java.time.Instant.parse(startDateStr);
                                startAt = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
                                System.out.println("SCHEDULE CAMPAIGN - Parsed Instant: " + startDateStr + " -> UTC: " + startAt);
                            } catch (Exception e2) {
                                // Fallback: parse as LocalDateTime (assume UTC if no timezone)
                                String cleaned = startDateStr.replace("Z", "").trim();
                                if (cleaned.endsWith("+00:00") || cleaned.endsWith("-00:00")) {
                                    cleaned = cleaned.substring(0, cleaned.length() - 6);
                                }
                                // Remove timezone offset if present (e.g., +05:00, -05:00)
                                if (cleaned.matches(".*[+-]\\d{2}:\\d{2}$")) {
                                    cleaned = cleaned.substring(0, cleaned.length() - 6);
                                }
                                
                                try {
                                    startAt = LocalDateTime.parse(cleaned);
                                    System.out.println("SCHEDULE CAMPAIGN - Parsed LocalDateTime (assumed UTC): " + cleaned + " -> " + startAt);
                                } catch (Exception e3) {
                                    // Try with seconds
                                    try {
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                                        startAt = LocalDateTime.parse(cleaned, formatter);
                                    } catch (Exception e4) {
                                        // Try with milliseconds
                                        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
                                        startAt = LocalDateTime.parse(cleaned, formatter2);
                                    }
                                }
                            }
                        }
                    } else {
                        // No time component, assume start of day in UTC
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        startAt = LocalDateTime.parse(startDateStr, formatter);
                        System.out.println("SCHEDULE CAMPAIGN - Parsed date only (assumed UTC midnight): " + startDateStr + " -> " + startAt);
                    }
                    
                    if (startAt != null) {
                        campaign.setStartAt(startAt);
                        System.out.println("SCHEDULE CAMPAIGN - Set start_at to UTC: " + startAt);
                        // Also log in EST for reference
                        ZonedDateTime estTime = startAt.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of("America/New_York"));
                        System.out.println("SCHEDULE CAMPAIGN - Which is EST/EDT: " + estTime.toLocalDateTime());
                    }
                } catch (Exception e) {
                    System.err.println("SCHEDULE CAMPAIGN - Error parsing start date: " + startDateStr + " - " + e.getMessage());
                    e.printStackTrace();
                    // Don't throw - just log and continue without start date
                }
            }
        }
        
        // Parse and save email delay (minimum 5 minutes)
        if (settings.containsKey("emailDelayMinutes")) {
            try {
                Object delayObj = settings.get("emailDelayMinutes");
                Integer delayMinutes = null;
                
                if (delayObj instanceof Integer) {
                    delayMinutes = (Integer) delayObj;
                } else if (delayObj instanceof String) {
                    delayMinutes = Integer.parseInt((String) delayObj);
                } else if (delayObj instanceof Number) {
                    delayMinutes = ((Number) delayObj).intValue();
                }
                
                // Enforce minimum of 5 minutes
                if (delayMinutes != null && delayMinutes >= 5) {
                    campaign.setEmailDelayMinutes(delayMinutes);
                    System.out.println("SCHEDULE CAMPAIGN - Set email_delay_minutes to: " + delayMinutes);
                } else if (delayMinutes != null && delayMinutes < 5) {
                    System.out.println("SCHEDULE CAMPAIGN - Email delay " + delayMinutes + " is below minimum, setting to 5 minutes");
                    campaign.setEmailDelayMinutes(5);
                } else {
                    // Default to 5 minutes if not provided or invalid
                    campaign.setEmailDelayMinutes(5);
                }
            } catch (Exception e) {
                System.err.println("SCHEDULE CAMPAIGN - Error parsing emailDelayMinutes: " + e.getMessage());
                // Default to 5 minutes on error
                campaign.setEmailDelayMinutes(5);
            }
        } else {
            // Default to 5 minutes if not provided
            if (campaign.getEmailDelayMinutes() == null) {
                campaign.setEmailDelayMinutes(5);
            }
        }
        
        // Save selected mailboxes
        if (settings.containsKey("selectedMailboxIds")) {
            Object mailboxIdsObj = settings.get("selectedMailboxIds");
            List<Integer> mailboxIds = new ArrayList<>();
            
            if (mailboxIdsObj instanceof List) {
                for (Object id : (List<?>) mailboxIdsObj) {
                    if (id instanceof Integer) {
                        mailboxIds.add((Integer) id);
                    } else if (id instanceof String) {
                        try {
                            mailboxIds.add(Integer.parseInt((String) id));
                        } catch (NumberFormatException e) {
                            System.err.println("SCHEDULE CAMPAIGN - Invalid mailbox ID: " + id);
                        }
                    }
                }
            }
            
            // Remove existing campaign-mailbox relationships
            campaignMailboxRepository.deleteByCampaignId(campaignId);
            
            // Create new relationships
            for (Integer mailboxId : mailboxIds) {
                CampaignMailboxEntity cm = new CampaignMailboxEntity(campaignId, mailboxId, 0, 1);
                campaignMailboxRepository.save(cm);
                System.out.println("SCHEDULE CAMPAIGN - Linked mailbox " + mailboxId + " to campaign " + campaignId);
            }
        }
        
        // Set status to scheduled if start date is in the future, or running if it's now/past
        if (campaign.getStartAt() != null) {
            if (campaign.getStartAt().isAfter(LocalDateTime.now())) {
                campaign.setStatus("scheduled");
            } else {
                campaign.setStatus("running");
            }
        } else {
            campaign.setStatus("draft");
        }
        
        campaignRepository.save(campaign);
        System.out.println("SCHEDULE CAMPAIGN - Campaign " + campaignId + " scheduled successfully");
    }
}

