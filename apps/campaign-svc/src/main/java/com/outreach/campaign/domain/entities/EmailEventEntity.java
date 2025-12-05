package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_events")
public class EmailEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "sent_email_id", nullable = false)
    private Integer sentEmailId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;  // opened, clicked, replied, bounced, unsubscribed
    
    @Column(name = "event_at")
    private LocalDateTime eventAt;
    
    @Column(name = "meta", columnDefinition = "JSONB")
    private String meta;  // JSON: ip, user_agent, link_url, etc.
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getSentEmailId() { return sentEmailId; }
    public void setSentEmailId(Integer sentEmailId) { this.sentEmailId = sentEmailId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
    
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
}

