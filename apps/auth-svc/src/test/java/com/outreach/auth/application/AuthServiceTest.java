package com.outreach.auth.application;

import com.outreach.auth.domain.AuthTokenResponse;
import com.outreach.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, emailService);
    }

    @Test
    void testRegister_Success() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        // When
        User result = authService.register("test@example.com", "password", "John", "Doe");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertFalse(result.getIsVerified());
        assertNotNull(result.getVerificationCode());
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testRegister_EmailExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () ->
            authService.register("test@example.com", "pass", "J", "D")
        );
    }

    @Test
    void testLogin_Success() {
        User u = new User("test@example.com", "encoded", "John", "Doe");
        u.setId(1L);
        u.setIsVerified(true);
        u.setEnabled(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtTokenProvider.generateToken("test@example.com", 1L)).thenReturn("token123");

        AuthTokenResponse response = authService.login("test@example.com", "password");
        assertEquals("token123", response.accessToken());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void testLogin_NotVerified() {
        User u = new User("test@example.com", "encoded", "John", "Doe");
        u.setIsVerified(false);
        u.setEnabled(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(u));
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.login("test@example.com", "password")
        );
        assertTrue(ex.getMessage().contains("verified"));
    }

    @Test
    void testLogin_InvalidPassword() {
        User u = new User("test@example.com", "encoded", "John", "Doe");
        u.setIsVerified(true);
        u.setEnabled(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            authService.login("test@example.com", "wrong")
        );
    }

    @Test
    void testVerify_Success() {
        User u = new User("test@example.com", "encoded", "John", "Doe");
        u.setVerificationCode("123456");
        u.setIsVerified(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(u));

        authService.verify("test@example.com", "123456");

        assertTrue(u.getIsVerified());
        assertNull(u.getVerificationCode());
        verify(userRepository).save(u);
    }
}
