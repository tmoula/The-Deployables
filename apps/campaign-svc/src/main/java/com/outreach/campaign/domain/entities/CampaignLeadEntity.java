package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_leads")
public class CampaignLeadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "campaign_id", nullable = false)
    private Integer campaignId;
    
    @Column(name = "lead_id", nullable = false)
    private Integer leadId;
    
    @Column(name = "status")
    private String status;  // queued, in_progress, completed, unsubscribed, bounced
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getCampaignId() { return campaignId; }
    public void setCampaignId(Integer campaignId) { this.campaignId = campaignId; }
    
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

