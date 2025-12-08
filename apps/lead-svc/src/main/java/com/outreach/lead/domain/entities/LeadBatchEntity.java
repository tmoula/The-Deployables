package com.outreach.lead.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lead_batches")
public class LeadBatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "icp_id")
    private Integer icpId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "requested_lead_count")
    private Integer requestedLeadCount;

    @Column(name = "total_leads")
    private Integer totalLeads;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "pending";
        }
        if (totalLeads == null) {
            totalLeads = 0;
        }
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getIcpId() { return icpId; }
    public void setIcpId(Integer icpId) { this.icpId = icpId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getRequestedLeadCount() { return requestedLeadCount; }
    public void setRequestedLeadCount(Integer requestedLeadCount) { this.requestedLeadCount = requestedLeadCount; }

    public Integer getTotalLeads() { return totalLeads; }
    public void setTotalLeads(Integer totalLeads) { this.totalLeads = totalLeads; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


