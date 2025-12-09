package com.outreach.auth.application;

import com.outreach.auth.domain.Mailbox;
import com.outreach.auth.domain.Mailbox.MailboxStatus;
import com.outreach.auth.infrastructure.GmailConnectionTester;
import jakarta.mail.MessagingException;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MailboxService {
    
    private final MailboxRepository mailboxRepository;
    private final GmailConnectionTester connectionTester;
    private final TextEncryptor encryptor;
    
    // Simple encryption key - in production, use environment variable
    private static final String ENCRYPTION_KEY = "change-this-encryption-key-prod";
    private static final String SALT = "deadbeef";
    
    public MailboxService(MailboxRepository mailboxRepository, GmailConnectionTester connectionTester) {
        this.mailboxRepository = mailboxRepository;
        this.connectionTester = connectionTester;
        this.encryptor = Encryptors.text(ENCRYPTION_KEY, SALT);
    }
    
    /**
     * Get all mailboxes for a user
     */
    public List<Mailbox> getUserMailboxes(Long userId) {
        return mailboxRepository.findByUserId(userId);
    }
    
    /**
     * Add a new Gmail mailbox
     * @throws IllegalArgumentException if connection test fails
     */
    @Transactional
    public Mailbox addMailbox(Long userId, String email, String displayName, String appPassword) {
        // Test connection first
        System.out.println("Adding mailbox: " + email + " for user " + userId);
        try {
            boolean connected = connectionTester.testConnection(email, appPassword);
            if (!connected) {
                System.err.println("Initial connection test failed for " + email);
                throw new IllegalArgumentException("Failed to connect to Gmail. Please check your credentials.");
            }
            System.out.println("Initial connection test successful");
        } catch (MessagingException e) {
            System.err.println("MessagingException during add: " + e.getMessage());
            throw new IllegalArgumentException("Failed to connect to Gmail: " + e.getMessage());
        }
        
        // Encrypt password
        String encryptedPassword = encryptor.encrypt(appPassword);
        
        // Create and save mailbox
        Mailbox mailbox = new Mailbox(userId, email, displayName, encryptedPassword);
        
        // Set required fields for new schema
        mailbox.setEmailAddress(email); // Set emailAddress field
        mailbox.setProvider("gmail"); // Default to gmail
        mailbox.setIsVerified(true); // Mark as verified after successful connection test
        
        // Extract domain from email
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.indexOf("@") + 1);
            mailbox.setDomain(domain);
        }
        
        System.out.println("Saving mailbox with provider: " + mailbox.getProvider() + ", emailAddress: " + mailbox.getEmailAddress());
        return mailboxRepository.save(mailbox);
    }
    
    /**
     * Update mailbox status (pause/resume)
     */
    @Transactional
    public Mailbox updateStatus(Long mailboxId, Long userId, MailboxStatus status) {
        System.out.println("Updating status for mailbox " + mailboxId + " to " + status);
        Mailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new IllegalArgumentException("Mailbox not found"));
        
        // Verify ownership
        if (!mailbox.getUserId().equals(userId)) {
            System.err.println("Unauthorized update attempt. Owner: " + mailbox.getUserId() + ", Requestor: " + userId);
            throw new IllegalArgumentException("Unauthorized access to mailbox");
        }
        
        mailbox.setStatus(status);
        Mailbox saved = mailboxRepository.save(mailbox);
        System.out.println("Status updated successfully");
        return saved;
    }
    
    /**
     * Delete a mailbox
     */
    @Transactional
    public void deleteMailbox(Long mailboxId, Long userId) {
        Mailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new IllegalArgumentException("Mailbox not found"));
        
        // Verify ownership
        if (!mailbox.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to mailbox");
        }
        
        mailboxRepository.delete(mailbox);
    }
    
    /**
     * Test mailbox connection
     */
    public boolean testMailboxConnection(Long mailboxId, Long userId) {
        System.out.println("Testing connection for mailbox " + mailboxId + " user " + userId);
        Mailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new IllegalArgumentException("Mailbox not found"));
        
        // Verify ownership
        if (!mailbox.getUserId().equals(userId)) {
            System.err.println("Unauthorized test attempt");
            throw new IllegalArgumentException("Unauthorized access to mailbox");
        }
        
        // Decrypt password and test
        try {
            String appPassword = encryptor.decrypt(mailbox.getEncryptedPassword());
            System.out.println("Password decrypted successfully. Testing IMAP...");
            boolean result = connectionTester.testConnection(mailbox.getEmail(), appPassword);
            System.out.println("Connection test result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
