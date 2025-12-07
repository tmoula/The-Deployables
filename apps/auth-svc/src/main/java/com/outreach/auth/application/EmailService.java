package com.outreach.auth.application;

import com.outreach.auth.api.dto.EmailDto;
import com.outreach.auth.domain.Mailbox;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    private final MailboxRepository mailboxRepository;
    private final TextEncryptor encryptor;

    // Using the same keys as MailboxService for compatibility
    private static final String ENCRYPTION_KEY = "change-this-encryption-key-prod";
    private static final String SALT = "deadbeef";

    public EmailService(MailboxRepository mailboxRepository) {
        this.mailboxRepository = mailboxRepository;
        this.encryptor = Encryptors.text(ENCRYPTION_KEY, SALT);
    }

    public List<EmailDto> fetchRecentEmails(Long userId) {
        List<Mailbox> mailboxes = mailboxRepository.findByUserId(userId);
        List<EmailDto> allEmails = new ArrayList<>();

        for (Mailbox mailbox : mailboxes) {
            try {
                allEmails.addAll(fetchFromMailbox(mailbox));
            } catch (Exception e) {
                System.err.println("Failed to fetch from " + mailbox.getEmail() + ": " + e.getMessage());
                // Continue with other mailboxes even if one fails
            }
        }

        // Sort by date, newest first
        allEmails.sort((e1, e2) -> e2.receivedAt().compareTo(e1.receivedAt()));
        return allEmails;
    }

    private List<EmailDto> fetchFromMailbox(Mailbox mailbox) throws Exception {
        System.out.println("Fetching emails from " + mailbox.getEmail());
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "15000");
        props.put("mail.imaps.connectiontimeout", "15000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        
        try {
            // Decrypt the password
            String appPassword = encryptor.decrypt(mailbox.getEncryptedPassword());
            
            store.connect("imap.gmail.com", mailbox.getEmail(), appPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Fetch last 20 messages
            int messageCount = inbox.getMessageCount();
            int start = Math.max(1, messageCount - 19);
            Message[] messages = inbox.getMessages(start, messageCount);

            List<EmailDto> emails = new ArrayList<>();
            // Iterate backwards to get newest first
            for (int i = messages.length - 1; i >= 0; i--) {
                Message msg = messages[i];
                
                String from = "Unknown";
                if (msg.getFrom() != null && msg.getFrom().length > 0) {
                    from = msg.getFrom()[0].toString();
                }

                LocalDateTime receivedAt = LocalDateTime.now();
                if (msg.getReceivedDate() != null) {
                    receivedAt = msg.getReceivedDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                }

                emails.add(new EmailDto(
                    String.valueOf(msg.getMessageNumber()), // Simple ID for now
                    msg.getSubject(),
                    from,
                    msg.getSubject(), // Using subject as snippet for simplicity
                    receivedAt,
                    mailbox.getEmail(),
                    msg.isSet(Flags.Flag.SEEN)
                ));
            }

            inbox.close(false);
            return emails;
        } finally {
            store.close();
        }
    }


    public void sendVerificationEmail(String to, String code) {
        // Simple implementation for now - in production use JavaMailSender
        System.out.println("Sending verification email to " + to + " with code " + code);
        // TODO: Implement actual sending logic using JavaMailSender if needed for this service
        // For now, we rely on the console log in AuthService for local dev
    }
}
