package com.outreach.auth.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mailboxes")
public class Mailbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "email_address", nullable = false)
    private String emailAddress;
    
    @Column(nullable = false)
    private String email; // Primary field - also used for email_address if not set
    
    @Column(name = "provider", nullable = false)
    private String provider = "gmail"; // gmail, outlook, smtp
    
    @Column(name = "display_name")
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailboxStatus status = MailboxStatus.ACTIVE;
    
    @Column(name = "encrypted_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedPassword;
    
    // SMTP/IMAP Configuration
    @Column(name = "smtp_host")
    private String smtpHost;
    
    @Column(name = "smtp_port")
    private Integer smtpPort = 587;
    
    @Column(name = "imap_host")
    private String imapHost;
    
    @Column(name = "imap_port")
    private Integer imapPort = 993;
    
    @Column(name = "use_tls")
    private Boolean useTls = true;
    
    @Column(name = "use_ssl")
    private Boolean useSsl = true;
    
    // OAuth Configuration
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "oauth_provider")
    private String oauthProvider = "none"; // gmail, outlook, none
    
    // Quota and Rate Limiting
    @Column(name = "daily_limit")
    private Integer dailyLimit = 50;
    
    @Column(name = "daily_sent")
    private Integer dailySent = 0;
    
    @Column(name = "last_reset_date")
    private LocalDate lastResetDate = LocalDate.now();
    
    // Metadata and Tracking
    @Column(name = "domain")
    private String domain;
    
    @Column(name = "rotation_group")
    private String rotationGroup;
    
    @Column(name = "reputation_score", precision = 5, scale = 2)
    private BigDecimal reputationScore = new BigDecimal("100.00");
    
    @Column(name = "bounce_rate", precision = 5, scale = 2)
    private BigDecimal bounceRate = new BigDecimal("0.00");
    
    @Column(name = "total_sent")
    private Integer totalSent = 0;
    
    @Column(name = "total_replies")
    private Integer totalReplies = 0;
    
    @Column(name = "total_bounces")
    private Integer totalBounces = 0;
    
    @Column(name = "warmup_status")
    private String warmupStatus = "none"; // none, running, paused
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public Mailbox() {}
    
    public Mailbox(Long userId, String email, String displayName, String encryptedPassword) {
        this.userId = userId;
        this.email = email;
        this.emailAddress = email; // Set both for compatibility
        this.displayName = displayName;
        this.encryptedPassword = encryptedPassword;
        this.provider = "gmail"; // Default to gmail
        // Set default SMTP/IMAP based on provider
        setDefaultSmtpImapSettings();
        // Extract domain from email
        if (email != null && email.contains("@")) {
            this.domain = email.substring(email.indexOf("@") + 1);
        }
    }
    
    private void setDefaultSmtpImapSettings() {
        if (provider == null || provider.equals("gmail")) {
            this.smtpHost = GMAIL_SMTP_HOST;
            this.smtpPort = GMAIL_SMTP_PORT;
            this.imapHost = GMAIL_IMAP_HOST;
            this.imapPort = GMAIL_IMAP_PORT;
            this.oauthProvider = "gmail";
        } else if (provider.equals("outlook")) {
            this.smtpHost = "smtp-mail.outlook.com";
            this.smtpPort = 587;
            this.imapHost = "outlook.office365.com";
            this.imapPort = 993;
            this.oauthProvider = "outlook";
        }
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
        // Return emailAddress if email is null (for backward compatibility)
        return email != null ? email : emailAddress;
    }
    
    public void setEmail(String email) {
        this.email = email;
        // Keep emailAddress in sync
        if (email != null && (emailAddress == null || emailAddress.isEmpty())) {
            this.emailAddress = email;
        }
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
    
    // New field getters and setters
    public String getEmailAddress() { 
        // Return email if emailAddress is null (for backward compatibility)
        return emailAddress != null ? emailAddress : email;
    }
    public void setEmailAddress(String emailAddress) { 
        this.emailAddress = emailAddress;
        // Keep legacy email field in sync
        if (emailAddress != null && (email == null || email.isEmpty())) {
            this.email = emailAddress;
        }
    }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { 
        this.provider = provider;
        setDefaultSmtpImapSettings();
    }
    
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    
    public String getImapHost() { return imapHost; }
    public void setImapHost(String imapHost) { this.imapHost = imapHost; }
    
    public Integer getImapPort() { return imapPort; }
    public void setImapPort(Integer imapPort) { this.imapPort = imapPort; }
    
    public Boolean getUseTls() { return useTls; }
    public void setUseTls(Boolean useTls) { this.useTls = useTls; }
    
    public Boolean getUseSsl() { return useSsl; }
    public void setUseSsl(Boolean useSsl) { this.useSsl = useSsl; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    
    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
    
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    
    public Integer getDailySent() { return dailySent; }
    public void setDailySent(Integer dailySent) { this.dailySent = dailySent; }
    
    public LocalDate getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(LocalDate lastResetDate) { this.lastResetDate = lastResetDate; }
    
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getRotationGroup() { return rotationGroup; }
    public void setRotationGroup(String rotationGroup) { this.rotationGroup = rotationGroup; }
    
    public BigDecimal getReputationScore() { return reputationScore; }
    public void setReputationScore(BigDecimal reputationScore) { this.reputationScore = reputationScore; }
    
    public BigDecimal getBounceRate() { return bounceRate; }
    public void setBounceRate(BigDecimal bounceRate) { this.bounceRate = bounceRate; }
    
    public Integer getTotalSent() { return totalSent; }
    public void setTotalSent(Integer totalSent) { this.totalSent = totalSent; }
    
    public Integer getTotalReplies() { return totalReplies; }
    public void setTotalReplies(Integer totalReplies) { this.totalReplies = totalReplies; }
    
    public Integer getTotalBounces() { return totalBounces; }
    public void setTotalBounces(Integer totalBounces) { this.totalBounces = totalBounces; }
    
    public String getWarmupStatus() { return warmupStatus; }
    public void setWarmupStatus(String warmupStatus) { this.warmupStatus = warmupStatus; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    // Helper method to check if daily quota is reached
    public boolean isDailyQuotaReached() {
        return dailySent != null && dailyLimit != null && dailySent >= dailyLimit;
    }
    
    // Helper method to reset daily sent count
    public void resetDailySent() {
        LocalDate today = LocalDate.now();
        if (lastResetDate == null || !lastResetDate.equals(today)) {
            this.dailySent = 0;
            this.lastResetDate = today;
        }
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
