package com.outreach.auth.domain;

public record AuthTokenResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    String email
) {}