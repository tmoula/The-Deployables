package com.outreach.lead.application;

import com.outreach.lead.domain.entities.UserSummary;
import com.outreach.lead.infrastructure.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {
    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Integer getUserIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email header (X-User-Email) is required");
        }

        UserSummary user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

        return user.getId();
    }
}


