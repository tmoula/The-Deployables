package com.outreach.auth.api;

import com.outreach.auth.api.dto.EmailDto;
import com.outreach.auth.application.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/inbox")
public class InboxController {

    private final EmailService emailService;

    public InboxController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmailDto>> getAllEmails(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EmailDto> emails = emailService.fetchRecentEmails(userId);
        return ResponseEntity.ok(emails);
    }
}
