// src/main/java/com/outreach/lead/LeadRequest.java
package com.outreach.lead.api.DTO;
import jakarta.validation.constraints.*;

import java.util.List;

public record LeadRequest(
        String id,
        @NotBlank String company,
        @NotBlank String domain,
        String role,
        String name,
        @Email(message="invalid email") String email,
        @Min(1) @Max(100000) Integer size,
        String region,
        List<String> stack
) {}
