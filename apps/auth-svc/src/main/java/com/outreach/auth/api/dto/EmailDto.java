package com.outreach.auth.api.dto;

import java.time.LocalDateTime;

public record EmailDto(
    String id,
    String subject,
    String from,
    String snippet,
    LocalDateTime receivedAt,
    String mailboxEmail,
    boolean isRead
) {}
