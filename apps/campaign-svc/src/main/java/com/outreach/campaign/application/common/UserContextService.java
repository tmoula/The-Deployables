package com.outreach.campaign.application.common;

import com.outreach.campaign.domain.entities.UserSummary;
import com.outreach.campaign.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {

    private static final Logger logger = LoggerFactory.getLogger(UserContextService.class);
    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolve user ID from email address.
     * For now we trust the frontend to send the correct email header.
     */
    public Integer getUserIdFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email header (X-User-Email) is required");
        }

        UserSummary user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

        logger.info("Resolved user email '{}' to user_id: {}", email, user.getId());
        return user.getId();
    }
}


