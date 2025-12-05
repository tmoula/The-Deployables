package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_steps")
public class CampaignStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "campaign_id", nullable = false)
    private Integer campaignId;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "delay_hours")
    private Integer delayHours;
    
    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;
    
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getCampaignId() { return campaignId; }
    public void setCampaignId(Integer campaignId) { this.campaignId = campaignId; }
    
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    
    public Integer getDelayHours() { return delayHours; }
    public void setDelayHours(Integer delayHours) { this.delayHours = delayHours; }
    
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

