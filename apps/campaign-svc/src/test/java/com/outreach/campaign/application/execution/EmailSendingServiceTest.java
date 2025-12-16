package com.outreach.campaign.application.execution;

import com.outreach.campaign.domain.entities.MailboxEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSendingServiceTest {

    @Spy
    private EmailSendingService emailSendingService;

    @Mock
    private MailboxEntity mailbox;

    @BeforeEach
    void setUp() {
        // Setup default mailbox behavior
        lenient().when(mailbox.getEmailAddress()).thenReturn("sender@example.com");
        lenient().when(mailbox.getSmtpHost()).thenReturn("smtp.example.com");
        lenient().when(mailbox.getSmtpPort()).thenReturn(587);
        lenient().when(mailbox.getDisplayName()).thenReturn("Sender Name");
    }

    @Test
    void testSendEmailSuccess() throws MessagingException {
        // Given
        doNothing().when(emailSendingService).sendTransport(any(MimeMessage.class));

        // When
        boolean result = emailSendingService.sendEmail(mailbox, "recipient@example.com", "Subject", "Body");

        // Then
        assertTrue(result);
        verify(emailSendingService, times(1)).sendTransport(any(MimeMessage.class));
    }

    @Test
    void testSendEmailFailure() throws MessagingException {
        // Given
        doThrow(new MessagingException("Connection failed")).when(emailSendingService).sendTransport(any(MimeMessage.class));

        // When
        boolean result = emailSendingService.sendEmail(mailbox, "recipient@example.com", "Subject", "Body");

        // Then
        assertFalse(result);
        verify(emailSendingService, times(1)).sendTransport(any(MimeMessage.class));
    }

    @Test
    void testSendEmailWithEncryptedPassword() throws MessagingException {
        // Given
        // Authenticator is not called when we mock sendTransport, so this stub is technically unnecessary
        // but we keep it lenient to document intent
        lenient().when(mailbox.getEncryptedPassword()).thenReturn("encrypted_pass");
        doNothing().when(emailSendingService).sendTransport(any(MimeMessage.class));

        // When
        boolean result = emailSendingService.sendEmail(mailbox, "recipient@example.com", "Subject", "Body");

        // Then
        assertTrue(result);
    }
    
    @Test
    void testSendEmailWithOAuthToken() throws MessagingException {
        // Given
        lenient().when(mailbox.getAccessToken()).thenReturn("some_token");
        doNothing().when(emailSendingService).sendTransport(any(MimeMessage.class));

        // When
        boolean result = emailSendingService.sendEmail(mailbox, "recipient@example.com", "Subject", "Body");

        // Then
        assertTrue(result);
    }

    @Test
    void testNeedsTokenRefreshTrue() {
        // Given
        MailboxEntity mailbox = new MailboxEntity();
        // Expired 6 minutes ago
        mailbox.setTokenExpiresAt(LocalDateTime.now().minusMinutes(6));

        // When
        boolean result = emailSendingService.needsTokenRefresh(mailbox);

        // Then
        assertTrue(result);
    }

    @Test
    void testNeedsTokenRefreshFalse() {
        // Given
        MailboxEntity mailbox = new MailboxEntity();
        // Expires in 10 minutes
        mailbox.setTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        // When
        boolean result = emailSendingService.needsTokenRefresh(mailbox);

        // Then
        assertFalse(result);
    }

    @Test
    void testNeedsTokenRefreshNull() {
        // Given
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setTokenExpiresAt(null);

        // When
        boolean result = emailSendingService.needsTokenRefresh(mailbox);

        // Then
        assertFalse(result);
    }
}
