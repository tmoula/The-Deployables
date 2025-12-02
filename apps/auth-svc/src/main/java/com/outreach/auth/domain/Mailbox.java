package com.outreach.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mailboxes")
public class Mailbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailboxStatus status = MailboxStatus.ACTIVE;
    
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public Mailbox() {}
    
    public Mailbox(Long userId, String email, String displayName, String encryptedPassword) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.encryptedPassword = encryptedPassword;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public MailboxStatus getStatus() {
        return status;
    }
    
    public void setStatus(MailboxStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Gmail IMAP/SMTP constants
    public static final String GMAIL_IMAP_HOST = "imap.gmail.com";
    public static final int GMAIL_IMAP_PORT = 993;
    public static final String GMAIL_SMTP_HOST = "smtp.gmail.com";
    public static final int GMAIL_SMTP_PORT = 587;
    
    public enum MailboxStatus {
        ACTIVE,
        PAUSED,
        DISCONNECTED
    }
}
