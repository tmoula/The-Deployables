package com.outreach.campaign.application.execution;

import com.outreach.campaign.application.lead.CampaignLeadService;
import com.outreach.campaign.application.rendering.EmailRenderService;
import com.outreach.campaign.domain.entities.*;
import com.outreach.campaign.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for executing campaigns and sending emails to leads
 */
@Service
public class CampaignExecutionService {
    
    private final CampaignRepository campaignRepository;
    private final CampaignLeadService campaignLeadService;
    private final EmailRenderService emailRenderService;
    private final EmailSendingService emailSendingService;
    private final MailboxRepository mailboxRepository;
    private final CampaignMailboxRepository campaignMailboxRepository;
    private final SentEmailRepository sentEmailRepository;
    
    // Track mailbox rotation (round-robin)
    private final Map<Integer, Integer> mailboxRotationIndex = new ConcurrentHashMap<>();
    
    public CampaignExecutionService(
        CampaignRepository campaignRepository,
        CampaignLeadService campaignLeadService,
        EmailRenderService emailRenderService,
        EmailSendingService emailSendingService,
        MailboxRepository mailboxRepository,
        CampaignMailboxRepository campaignMailboxRepository,
        SentEmailRepository sentEmailRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignLeadService = campaignLeadService;
        this.emailRenderService = emailRenderService;
        this.emailSendingService = emailSendingService;
        this.mailboxRepository = mailboxRepository;
        this.campaignMailboxRepository = campaignMailboxRepository;
        this.sentEmailRepository = sentEmailRepository;
    }
    
    /**
     * Execute a campaign - send emails to all queued leads
     */
    @Transactional
    public void executeCampaign(Integer campaignId) {
        System.out.println("EXECUTE CAMPAIGN - Starting execution for campaign_id: " + campaignId);
        
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found: " + campaignId));
        
        // Check if campaign has email templates
        if (campaign.getEmailSubject() == null || campaign.getEmailSubject().trim().isEmpty() ||
            campaign.getEmailBody() == null || campaign.getEmailBody().trim().isEmpty()) {
            System.err.println("EXECUTE CAMPAIGN - Campaign " + campaignId + " has no email templates");
            campaign.setStatus("draft");
            campaignRepository.save(campaign);
            return;
        }
        
        // Get mailboxes for this campaign
        List<CampaignMailboxEntity> campaignMailboxes = campaignMailboxRepository
            .findByCampaignIdOrderByPriorityDesc(campaignId);
        
        if (campaignMailboxes.isEmpty()) {
            System.err.println("EXECUTE CAMPAIGN - Campaign " + campaignId + " has no mailboxes selected");
            campaign.setStatus("draft");
            campaignRepository.save(campaign);
            return;
        }
        
        // Get all leads for this campaign
        List<LeadEntity> leads = campaignLeadService.getCampaignLeads(campaignId);
        
        if (leads.isEmpty()) {
            System.err.println("EXECUTE CAMPAIGN - Campaign " + campaignId + " has no leads");
            campaign.setStatus("completed");
            campaignRepository.save(campaign);
            return;
        }
        
        System.out.println("EXECUTE CAMPAIGN - Found " + leads.size() + " leads and " + campaignMailboxes.size() + " mailboxes");
        
        // Update campaign status to running
        campaign.setStatus("running");
        campaignRepository.save(campaign);
        
        // Get CSV columns for variable replacement
        List<String> csvColumns = new ArrayList<>();
        if (campaign.getCsvColumns() != null && !campaign.getCsvColumns().trim().isEmpty()) {
            csvColumns = emailRenderService.parseCsvColumns(campaign.getCsvColumns());
        }
        
        int sentCount = 0;
        int failedCount = 0;
        
        // Send email to each lead
        for (LeadEntity lead : leads) {
            try {
                // Skip if lead has no email
                if (lead.getEmail() == null || lead.getEmail().trim().isEmpty()) {
                    System.out.println("EXECUTE CAMPAIGN - Skipping lead " + lead.getId() + " (no email)");
                    continue;
                }
                
                // Check if email already sent or queued for this lead
                // This prevents duplicate emails if campaign is executed multiple times
                List<SentEmailEntity> existingSent = sentEmailRepository.findByCampaignIdAndLeadId(
                    campaignId, lead.getId());
                boolean alreadySentOrQueued = existingSent.stream()
                    .anyMatch(e -> "sent".equals(e.getStatus()) || "queued".equals(e.getStatus()));
                
                if (alreadySentOrQueued) {
                    System.out.println("EXECUTE CAMPAIGN - Skipping lead " + lead.getId() + " (email already sent or queued)");
                    continue;
                }
                
                // Select mailbox (round-robin)
                MailboxEntity mailbox = selectMailbox(campaignId, campaignMailboxes);
                if (mailbox == null) {
                    System.err.println("EXECUTE CAMPAIGN - No available mailbox for campaign " + campaignId);
                    failedCount++;
                    continue;
                }
                
                // Render email templates with lead data
                String renderedSubject = emailRenderService.renderEmail(
                    campaign.getEmailSubject(), lead, csvColumns, System.currentTimeMillis());
                String renderedBody = emailRenderService.renderEmail(
                    campaign.getEmailBody(), lead, csvColumns, System.currentTimeMillis());
                
                // Create sent_email record (queued)
                SentEmailEntity sentEmail = new SentEmailEntity();
                sentEmail.setCampaignId(campaignId);
                sentEmail.setLeadId(lead.getId());
                sentEmail.setMailboxId(mailbox.getId());
                sentEmail.setToEmail(lead.getEmail());
                sentEmail.setSubject(renderedSubject);
                sentEmail.setBody(renderedBody);
                sentEmail.setStatus("queued");
                sentEmail.setCreatedAt(LocalDateTime.now());
                sentEmail = sentEmailRepository.save(sentEmail);
                
                // Send email
                boolean success = emailSendingService.sendEmail(mailbox, lead.getEmail(), renderedSubject, renderedBody);
                
                if (success) {
                    sentEmail.setStatus("sent");
                    sentEmail.setSentAt(LocalDateTime.now());
                    sentEmailRepository.save(sentEmail);
                    sentCount++;
                    System.out.println("EXECUTE CAMPAIGN - Sent email to " + lead.getEmail());
                } else {
                    sentEmail.setStatus("failed");
                    sentEmailRepository.save(sentEmail);
                    failedCount++;
                    System.err.println("EXECUTE CAMPAIGN - Failed to send email to " + lead.getEmail());
                }
                
                // Delay between emails (configurable, minimum 5 minutes)
                int delayMinutes = campaign.getEmailDelayMinutes() != null && campaign.getEmailDelayMinutes() >= 5
                    ? campaign.getEmailDelayMinutes()
                    : 5; // Default to 5 minutes minimum
                
                long delayMillis = delayMinutes * 60 * 1000L; // Convert minutes to milliseconds
                
                // Don't delay after the last email
                if (sentCount + failedCount < leads.size()) {
                    System.out.println("EXECUTE CAMPAIGN - Waiting " + delayMinutes + " minutes before next email...");
                    Thread.sleep(delayMillis);
                }
                
            } catch (Exception e) {
                System.err.println("EXECUTE CAMPAIGN - Error processing lead " + lead.getId() + ": " + e.getMessage());
                e.printStackTrace();
                failedCount++;
            }
        }
        
        // Update campaign status
        if (sentCount > 0) {
            campaign.setStatus("completed");
            System.out.println("EXECUTE CAMPAIGN - Completed: " + sentCount + " sent, " + failedCount + " failed");
        } else {
            campaign.setStatus("failed");
            System.err.println("EXECUTE CAMPAIGN - Failed: No emails sent");
        }
        campaignRepository.save(campaign);
    }
    
    /**
     * Select a mailbox for sending (round-robin based on rotation weight)
     */
    private MailboxEntity selectMailbox(Integer campaignId, List<CampaignMailboxEntity> campaignMailboxes) {
        if (campaignMailboxes.isEmpty()) {
            return null;
        }
        
        // Get current rotation index for this campaign
        int currentIndex = mailboxRotationIndex.getOrDefault(campaignId, 0);
        
        // Select mailbox based on rotation weight
        int totalWeight = campaignMailboxes.stream()
            .mapToInt(CampaignMailboxEntity::getRotationWeight)
            .sum();
        
        if (totalWeight == 0) {
            // Simple round-robin if no weights
            CampaignMailboxEntity selected = campaignMailboxes.get(currentIndex % campaignMailboxes.size());
            mailboxRotationIndex.put(campaignId, (currentIndex + 1) % campaignMailboxes.size());
            return mailboxRepository.findById(selected.getMailboxId()).orElse(null);
        }
        
        // Weighted round-robin
        int target = currentIndex % totalWeight;
        int accumulated = 0;
        for (CampaignMailboxEntity cm : campaignMailboxes) {
            accumulated += cm.getRotationWeight();
            if (target < accumulated) {
                mailboxRotationIndex.put(campaignId, (currentIndex + 1) % totalWeight);
                return mailboxRepository.findById(cm.getMailboxId()).orElse(null);
            }
        }
        
        // Fallback to first mailbox
        return mailboxRepository.findById(campaignMailboxes.get(0).getMailboxId()).orElse(null);
    }
}

