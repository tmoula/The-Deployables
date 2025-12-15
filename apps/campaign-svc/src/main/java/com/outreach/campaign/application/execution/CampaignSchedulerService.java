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
                    // Compare UTC times - execute if start time has passed or is within 10 seconds
                    // The scheduler runs every minute, so we allow a small window to catch campaigns
                    // that should have started in the last minute
                    long secondsDiff = java.time.Duration.between(startAt, now).getSeconds();
                    boolean shouldExecute = secondsDiff >= 0 && secondsDiff <= 60; // Within last minute
                    
                    if (shouldExecute) {
                        // CRITICAL: Change status to "running" IMMEDIATELY to prevent duplicate executions
                        // This must happen synchronously before starting the thread
                        campaign.setStatus("running");
                        campaignRepository.save(campaign);
                        
                        System.out.println("SCHEDULER - Campaign " + campaign.getId() + " is ready to execute (start_at UTC: " + 
                            startAt + ", now UTC: " + now + ")");
                        System.out.println("SCHEDULER - Changed campaign status to 'running' to prevent duplicate execution");
                    
                        // Execute the campaign in a separate thread to avoid blocking
                        new Thread(() -> {
                            try {
                                campaignExecutionService.executeCampaign(campaign.getId());
                            } catch (Exception e) {
                                System.err.println("SCHEDULER - Error executing campaign " + campaign.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                                // Mark campaign as failed
                                try {
                                    CampaignEntity failedCampaign = campaignRepository.findById(campaign.getId()).orElse(null);
                                    if (failedCampaign != null) {
                                        failedCampaign.setStatus("failed");
                                        campaignRepository.save(failedCampaign);
                                    }
                                } catch (Exception saveError) {
                                    System.err.println("SCHEDULER - Error saving failed status: " + saveError.getMessage());
                                }
                            }
                        }).start();
                    } else {
                        // Log when campaign will execute (for debugging)
                        if (startAt.isBefore(now.plusMinutes(5))) {
                            long minutesUntil = java.time.Duration.between(now, startAt).toMinutes();
                            long secondsUntil = java.time.Duration.between(now, startAt).getSeconds() % 60;
                            System.out.println("SCHEDULER - Campaign " + campaign.getId() + " will execute in " + 
                                minutesUntil + " minutes " + secondsUntil + " seconds (start_at UTC: " + 
                                startAt + ", now UTC: " + now + ")");
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

