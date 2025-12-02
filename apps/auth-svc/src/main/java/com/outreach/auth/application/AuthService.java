package com.outreach.auth.application;

import com.outreach.auth.domain.AuthTokenResponse;
import com.outreach.auth.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.outreach.auth.infrastructure.EmailService emailService;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, com.outreach.auth.infrastructure.EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }
    
    public User register(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(email, encodedPassword, firstName, lastName);
        
        // Generate 6-digit verification code
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setVerificationCode(code);
        user.setIsVerified(false);
        
        User savedUser = userRepository.save(user);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(email, code);
        } catch (Exception e) {
            // Log error but don't fail registration? Or fail?
            // For now, let's log and continue, user can request resend later (if we implement resend)
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
        
        return savedUser;
    }
    
    public AuthTokenResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("Email not verified. Please check your email for the verification code.");
        }
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        String token = jwtTokenProvider.generateToken(email);
        return new AuthTokenResponse(token, "Bearer", 86400L, email);
    }

    public void verify(String email, String code) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            return; // Already verified
        }
        
        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
            user.setIsVerified(true);
            user.setVerificationCode(null); // Clear code after use
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid verification code");
        }
    }
    
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
    
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }
}