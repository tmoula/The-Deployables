package com.outreach.campaign.application.execution;

import com.outreach.campaign.domain.entities.MailboxEntity;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Service for sending emails via SMTP using Gmail OAuth or app passwords
 */
@Service
public class EmailSendingService {
    
    private static final String ENCRYPTION_KEY = "change-this-encryption-key-prod";
    private static final String SALT = "deadbeef";
    private final TextEncryptor encryptor;
    
    public EmailSendingService() {
        this.encryptor = Encryptors.text(ENCRYPTION_KEY, SALT);
    }
    
    /**
     * Send an email using the specified mailbox
     * @param mailbox The mailbox to send from
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param body Email body (HTML supported)
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEmail(MailboxEntity mailbox, String toEmail, String subject, String body) {
        try {
            System.out.println("SENDING EMAIL - From: " + mailbox.getEmailAddress() + ", To: " + toEmail);
            System.out.println("SENDING EMAIL - Subject: " + subject);
            
            // Get SMTP configuration
            String smtpHost = mailbox.getSmtpHost() != null ? mailbox.getSmtpHost() : "smtp.gmail.com";
            int smtpPort = mailbox.getSmtpPort() != null ? mailbox.getSmtpPort() : 587;
            
            // Create mail session properties
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            
            // Create session with authenticator
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // Try OAuth first, fall back to app password
                    if (mailbox.getAccessToken() != null && !mailbox.getAccessToken().isEmpty()) {
                        // For OAuth, we'd need to use OAuth2TokenSource
                        // For now, fall back to app password
                        System.out.println("SENDING EMAIL - OAuth token found, but using app password for now");
                    }
                    
                    // Use app password (decrypt if encrypted)
                    String password = mailbox.getEncryptedPassword();
                    if (password != null && !password.isEmpty()) {
                        try {
                            password = encryptor.decrypt(password);
                        } catch (Exception e) {
                            // If decryption fails, assume it's already plain text
                            System.out.println("SENDING EMAIL - Password decryption failed, using as-is");
                        }
                    }
                    
                    return new PasswordAuthentication(mailbox.getEmailAddress(), password);
                }
            });
            
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailbox.getEmailAddress(), 
                mailbox.getDisplayName() != null ? mailbox.getDisplayName() : mailbox.getEmailAddress()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");
            
            // Send message
            Transport.send(message);
            
            System.out.println("SENDING EMAIL - SUCCESS: Email sent to " + toEmail);
            return true;
            
        } catch (Exception e) {
            System.err.println("SENDING EMAIL - ERROR: Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if mailbox OAuth token needs refresh
     */
    public boolean needsTokenRefresh(MailboxEntity mailbox) {
        if (mailbox.getTokenExpiresAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(mailbox.getTokenExpiresAt().minusMinutes(5));
    }
}

