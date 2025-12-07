package com.outreach.auth.api;

import com.outreach.auth.api.dto.AddMailboxRequest;
import com.outreach.auth.api.dto.MailboxResponse;
import com.outreach.auth.application.MailboxService;
import com.outreach.auth.domain.Mailbox;
import com.outreach.auth.domain.Mailbox.MailboxStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth/mailboxes")
public class MailboxController {
    
    private final MailboxService mailboxService;
    
    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }
    
    /**
     * Get all mailboxes for the authenticated user
     * For now, we'll extract userId from a header (in production, use JWT)
     */
    @GetMapping
    public ResponseEntity<List<MailboxResponse>> getMailboxes(
            @RequestAttribute(value = "userId", required = false) Long jwtUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        
        Long userId = jwtUserId != null ? jwtUserId : headerUserId;

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<MailboxResponse> mailboxes = mailboxService.getUserMailboxes(userId)
            .stream()
            .map(MailboxResponse::fromEntity)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(mailboxes);
    }
    
    /**
     * Add a new Gmail mailbox
     */
    @PostMapping
    public ResponseEntity<?> addMailbox(
            @RequestAttribute(value = "userId", required = false) Long jwtUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody AddMailboxRequest request) {
        
        Long userId = jwtUserId != null ? jwtUserId : headerUserId;

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Mailbox mailbox = mailboxService.addMailbox(
                userId,
                request.email(),
                request.displayName(),
                request.appPassword()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(MailboxResponse.fromEntity(mailbox));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update mailbox status (pause/resume)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestAttribute(value = "userId", required = false) Long jwtUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody Map<String, String> body) {
        
        Long userId = jwtUserId != null ? jwtUserId : headerUserId;

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String statusStr = body.get("status");
            MailboxStatus status = MailboxStatus.valueOf(statusStr.toUpperCase());
            
            Mailbox mailbox = mailboxService.updateStatus(id, userId, status);
            return ResponseEntity.ok(MailboxResponse.fromEntity(mailbox));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete a mailbox
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMailbox(
            @PathVariable Long id,
            @RequestAttribute(value = "userId", required = false) Long jwtUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        
        Long userId = jwtUserId != null ? jwtUserId : headerUserId;

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            mailboxService.deleteMailbox(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test mailbox connection
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testConnection(
            @PathVariable Long id,
            @RequestAttribute(value = "userId", required = false) Long jwtUserId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        
        Long userId = jwtUserId != null ? jwtUserId : headerUserId;

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            boolean connected = mailboxService.testMailboxConnection(id, userId);
            return ResponseEntity.ok(Map.of(
                "connected", connected,
                "message", connected ? "Connection successful" : "Connection failed"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
