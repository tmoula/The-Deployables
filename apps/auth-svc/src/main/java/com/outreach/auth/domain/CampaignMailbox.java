package com.outreach.auth.domain;

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
public class CampaignMailbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;
    
    @Column(name = "mailbox_id", nullable = false)
    private Long mailboxId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mailbox_id", insertable = false, updatable = false)
    private Mailbox mailbox;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // Higher priority = used first
    
    @Column(name = "rotation_weight", nullable = false)
    private Integer rotationWeight = 1; // Weight for round-robin rotation
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public CampaignMailbox() {}
    
    public CampaignMailbox(Long campaignId, Long mailboxId, Integer priority, Integer rotationWeight) {
        this.campaignId = campaignId;
        this.mailboxId = mailboxId;
        this.priority = priority;
        this.rotationWeight = rotationWeight;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    
    public Long getMailboxId() { return mailboxId; }
    public void setMailboxId(Long mailboxId) { this.mailboxId = mailboxId; }
    
    public Mailbox getMailbox() { return mailbox; }
    public void setMailbox(Mailbox mailbox) { this.mailbox = mailbox; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public Integer getRotationWeight() { return rotationWeight; }
    public void setRotationWeight(Integer rotationWeight) { this.rotationWeight = rotationWeight; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}



