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
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    public User register(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(email, encodedPassword, firstName, lastName);
        return userRepository.save(user);
    }
    
    public AuthTokenResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        String token = jwtTokenProvider.generateToken(email);
        return new AuthTokenResponse(token, "Bearer", 86400L, email);
    }
    
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
    
    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }
}