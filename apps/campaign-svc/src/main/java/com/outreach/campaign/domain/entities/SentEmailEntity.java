package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sent_emails")
public class SentEmailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "campaign_id", nullable = false)
    private Integer campaignId;
    
    @Column(name = "campaign_step_id")
    private Integer campaignStepId;
    
    @Column(name = "lead_id", nullable = false)
    private Integer leadId;
    
    @Column(name = "mailbox_id")
    private Integer mailboxId;
    
    @Column(name = "to_email", nullable = false)
    private String toEmail;
    
    @Column(name = "subject", nullable = false, columnDefinition = "TEXT")
    private String subject;
    
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;
    
    @Column(name = "status")
    private String status;  // queued, sent, failed
    
    @Column(name = "provider_message_id")
    private String providerMessageId;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getCampaignId() { return campaignId; }
    public void setCampaignId(Integer campaignId) { this.campaignId = campaignId; }
    
    public Integer getCampaignStepId() { return campaignStepId; }
    public void setCampaignStepId(Integer campaignStepId) { this.campaignStepId = campaignStepId; }
    
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    
    public Integer getMailboxId() { return mailboxId; }
    public void setMailboxId(Integer mailboxId) { this.mailboxId = mailboxId; }
    
    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

