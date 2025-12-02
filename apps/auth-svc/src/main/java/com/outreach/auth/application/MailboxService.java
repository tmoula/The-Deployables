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
        try {
            boolean connected = connectionTester.testConnection(email, appPassword);
            if (!connected) {
                throw new IllegalArgumentException("Failed to connect to Gmail. Please check your credentials.");
            }
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Failed to connect to Gmail: " + e.getMessage());
        }
        
        // Encrypt password
        String encryptedPassword = encryptor.encrypt(appPassword);
        
        // Create and save mailbox
        Mailbox mailbox = new Mailbox(userId, email, displayName, encryptedPassword);
        return mailboxRepository.save(mailbox);
    }
    
    /**
     * Update mailbox status (pause/resume)
     */
    @Transactional
    public Mailbox updateStatus(Long mailboxId, Long userId, MailboxStatus status) {
        Mailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new IllegalArgumentException("Mailbox not found"));
        
        // Verify ownership
        if (!mailbox.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to mailbox");
        }
        
        mailbox.setStatus(status);
        return mailboxRepository.save(mailbox);
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
        Mailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new IllegalArgumentException("Mailbox not found"));
        
        // Verify ownership
        if (!mailbox.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to mailbox");
        }
        
        // Decrypt password and test
        String appPassword = encryptor.decrypt(mailbox.getEncryptedPassword());
        try {
            return connectionTester.testConnection(mailbox.getEmail(), appPassword);
        } catch (MessagingException e) {
            return false;
        }
    }
}
