package com.outreach.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMailboxRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    String displayName,
    
    @NotBlank(message = "App password is required")
    String appPassword
) {}
