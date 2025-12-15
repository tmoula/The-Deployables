package com.outreach.lead.application;

import com.outreach.lead.domain.entities.UserSummary;
import com.outreach.lead.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserContextService userContextService;

    private UserSummary testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSummary();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetUserIdFromEmail_WithValidEmail_ReturnsUserId() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        Integer result = userContextService.getUserIdFromEmail("test@example.com");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result);
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testGetUserIdFromEmail_WithNullEmail_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail(null);
        });
        
        assertTrue(exception.getMessage().contains("User email header (X-User-Email) is required"));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testGetUserIdFromEmail_WithBlankEmail_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail("   ");
        });
        
        assertTrue(exception.getMessage().contains("User email header (X-User-Email) is required"));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testGetUserIdFromEmail_WithEmptyEmail_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail("");
        });
        
        assertTrue(exception.getMessage().contains("User email header (X-User-Email) is required"));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void testGetUserIdFromEmail_WithNonExistentEmail_ThrowsException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail("nonexistent@example.com");
        });
        
        assertTrue(exception.getMessage().contains("User not found for email"));
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }
}

