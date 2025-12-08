package com.outreach.campaign.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class CampaignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "icp_id")
    private Integer icpId;
    
    @Column(name = "lead_batch_id")
    private Integer leadBatchId;
    
    @Column(name = "mailbox_id")
    private Integer mailboxId;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "start_at")
    private LocalDateTime startAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "csv_filename")
    private String csvFilename;
    
    @Column(name = "csv_columns", columnDefinition = "TEXT")
    private String csvColumns; // JSON array of column names
    

    @Column(name = "email_subject", columnDefinition = "TEXT")
    private String emailSubject;

    @Column(name = "email_body", columnDefinition = "TEXT")
    private String emailBody;


    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getIcpId() { return icpId; }
    public void setIcpId(Integer icpId) { this.icpId = icpId; }
    
    public Integer getLeadBatchId() { return leadBatchId; }
    public void setLeadBatchId(Integer leadBatchId) { this.leadBatchId = leadBatchId; }
    
    public Integer getMailboxId() { return mailboxId; }
    public void setMailboxId(Integer mailboxId) { this.mailboxId = mailboxId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCsvFilename() { return csvFilename; }
    public void setCsvFilename(String csvFilename) { this.csvFilename = csvFilename; }
    
    public String getCsvColumns() { return csvColumns; }
    public void setCsvColumns(String csvColumns) { this.csvColumns = csvColumns; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getEmailBody() { return emailBody; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }

}




