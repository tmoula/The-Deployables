package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Many-to-many relationship between campaigns and mailboxes
 * Allows campaigns to use multiple mailboxes with rotation and priority
 */
@Entity
@Table(name = "campaign_mailboxes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"campaign_id", "mailbox_id"})
})
public class CampaignMailboxEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "campaign_id", nullable = false)
    private Integer campaignId;
    
    @Column(name = "mailbox_id", nullable = false)
    private Integer mailboxId;
    
    @Column(name = "priority")
    private Integer priority = 0; // Higher priority = used first
    
    @Column(name = "rotation_weight")
    private Integer rotationWeight = 1; // Weight for round-robin rotation
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public CampaignMailboxEntity() {}
    
    public CampaignMailboxEntity(Integer campaignId, Integer mailboxId, Integer priority, Integer rotationWeight) {
        this.campaignId = campaignId;
        this.mailboxId = mailboxId;
        this.priority = priority;
        this.rotationWeight = rotationWeight;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getCampaignId() { return campaignId; }
    public void setCampaignId(Integer campaignId) { this.campaignId = campaignId; }
    
    public Integer getMailboxId() { return mailboxId; }
    public void setMailboxId(Integer mailboxId) { this.mailboxId = mailboxId; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public Integer getRotationWeight() { return rotationWeight; }
    public void setRotationWeight(Integer rotationWeight) { this.rotationWeight = rotationWeight; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

