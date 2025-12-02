package com.outreach.auth.api.dto;

import com.outreach.auth.domain.Mailbox;

public record MailboxResponse(
    Long id,
    String email,
    String displayName,
    String status,
    String createdAt,
    String updatedAt
) {
    public static MailboxResponse fromEntity(Mailbox mailbox) {
        return new MailboxResponse(
            mailbox.getId(),
            mailbox.getEmail(),
            mailbox.getDisplayName(),
            mailbox.getStatus().name(),
            mailbox.getCreatedAt().toString(),
            mailbox.getUpdatedAt().toString()
        );
    }
}
