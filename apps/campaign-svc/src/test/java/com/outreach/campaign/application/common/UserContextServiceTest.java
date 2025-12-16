package com.outreach.campaign.application.common;

import com.outreach.campaign.domain.entities.UserSummary;
import com.outreach.campaign.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserContextService userContextService;

    @BeforeEach
    void setUp() {
        userContextService = new UserContextService(userRepository);
    }

    @Test
    void testGetUserIdFromEmail_Success() {
        UserSummary user = new UserSummary();
        user.setId(1);
        user.setEmail("test@test.com");
        
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        
        Integer userId = userContextService.getUserIdFromEmail("test@test.com");
        
        assertEquals(1, userId);
    }

    @Test
    void testGetUserIdFromEmail_NullEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail(null);
        });
        assertEquals("User email header (X-User-Email) is required", exception.getMessage());
    }

    @Test
    void testGetUserIdFromEmail_EmptyEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail("  ");
        });
        assertEquals("User email header (X-User-Email) is required", exception.getMessage());
    }

    @Test
    void testGetUserIdFromEmail_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userContextService.getUserIdFromEmail("unknown@test.com");
        });
        assertTrue(exception.getMessage().contains("User not found"));
    }
}
