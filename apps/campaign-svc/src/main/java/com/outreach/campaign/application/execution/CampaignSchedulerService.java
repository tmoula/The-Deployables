package com.outreach.campaign.application.execution;

import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.infrastructure.CampaignRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Scheduled service that checks for campaigns ready to execute
 * Runs every minute to check for campaigns that should start sending
 */
@Service
public class CampaignSchedulerService {
    
    private final CampaignRepository campaignRepository;
    private final CampaignExecutionService campaignExecutionService;
    
    public CampaignSchedulerService(
        CampaignRepository campaignRepository,
        CampaignExecutionService campaignExecutionService
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignExecutionService = campaignExecutionService;
    }
    
    /**
     * Check for campaigns that are scheduled and ready to execute
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds (1 minute)
    public void checkScheduledCampaigns() {
        try {
            // Use UTC for comparison (database stores UTC)
            ZonedDateTime nowUTC = ZonedDateTime.now(ZoneId.of("UTC"));
            LocalDateTime now = nowUTC.toLocalDateTime();
            
            // Find campaigns that are scheduled and should start now
            List<CampaignEntity> scheduledCampaigns = campaignRepository.findByStatus("scheduled");
            
            for (CampaignEntity campaign : scheduledCampaigns) {
                // Check if it's time to start this campaign
                // campaign.getStartAt() is stored in UTC
                if (campaign.getStartAt() != null) {
                    LocalDateTime startAt = campaign.getStartAt();
                    // Compare UTC times
                    if (startAt.isBefore(now) || startAt.isEqual(now)) {
                    
                        System.out.println("SCHEDULER - Campaign " + campaign.getId() + " is ready to execute (start_at UTC: " + 
                            startAt + ", now UTC: " + now + ")");
                    
                    // Execute the campaign in a separate thread to avoid blocking
                    new Thread(() -> {
                        try {
                            campaignExecutionService.executeCampaign(campaign.getId());
                        } catch (Exception e) {
                            System.err.println("SCHEDULER - Error executing campaign " + campaign.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                            // Mark campaign as failed
                            campaign.setStatus("failed");
                            campaignRepository.save(campaign);
                        }
                    }).start();
                    } else {
                        // Log when campaign will execute (for debugging)
                        if (startAt.isBefore(now.plusMinutes(5))) {
                            System.out.println("SCHEDULER - Campaign " + campaign.getId() + " will execute soon (start_at UTC: " + 
                                startAt + ", now UTC: " + now + ", wait: " + 
                                java.time.Duration.between(now, startAt).toMinutes() + " minutes)");
                        }
                    }
                }
            }
            
            // Also check for running campaigns that might need to continue
            // (for follow-up emails, etc.)
            // For now, we'll handle this in the execution service
            
        } catch (Exception e) {
            System.err.println("SCHEDULER - Error checking scheduled campaigns: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

