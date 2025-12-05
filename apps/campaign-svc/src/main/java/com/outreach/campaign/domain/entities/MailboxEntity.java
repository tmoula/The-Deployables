package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mailboxes")
public class MailboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "email_address", nullable = false)
    private String emailAddress;
    
    @Column(name = "provider", nullable = false)
    private String provider;  // gmail, outlook, smtp
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "is_verified")
    private Boolean isVerified;
    
    @Column(name = "warmup_status")
    private String warmupStatus;  // none, running, paused
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public String getWarmupStatus() { return warmupStatus; }
    public void setWarmupStatus(String warmupStatus) { this.warmupStatus = warmupStatus; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

