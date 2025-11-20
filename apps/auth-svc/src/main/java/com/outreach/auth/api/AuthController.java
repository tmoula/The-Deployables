package com.outreach.auth.api;

import com.outreach.auth.api.dto.LoginRequest;
import com.outreach.auth.api.dto.RegisterRequest;
import com.outreach.auth.application.AuthService;
import com.outreach.auth.domain.AuthTokenResponse;
import com.outreach.auth.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req.email(), req.password(), req.firstName(), req.lastName());
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthTokenResponse token = authService.login(req.email(), req.password());
        return ResponseEntity.ok(token);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(false);
        }
        
        String token = authHeader.substring(7);
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(isValid);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}