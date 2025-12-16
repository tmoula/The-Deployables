package com.outreach.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenResponseTest {

    @Test
    void testAuthTokenResponseConstructor() {
        // Record parameters: accessToken, tokenType, expiresIn, email, firstName, lastName, userId
        AuthTokenResponse response = new AuthTokenResponse(
            "test-token",
            "Bearer",
            3600L,
            "test@example.com",
            "John",
            "Doe",
            1L
        );
        
        assertEquals("test-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("test@example.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals(1L, response.userId());
    }

    @Test
    void testAuthTokenResponseWithNullValues() {
        AuthTokenResponse response = new AuthTokenResponse(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        assertNull(response.accessToken());
        assertNull(response.tokenType());
        assertNull(response.expiresIn());
        assertNull(response.email());
        assertNull(response.firstName());
        assertNull(response.lastName());
        assertNull(response.userId());
    }

    @Test
    void testAuthTokenResponseEquality() {
        AuthTokenResponse response1 = new AuthTokenResponse(
            "token",
            "Bearer",
            3600L,
            "test@example.com",
            "John",
            "Doe",
            1L
        );
        
        AuthTokenResponse response2 = new AuthTokenResponse(
            "token",
            "Bearer",
            3600L,
            "test@example.com",
            "John",
            "Doe",
            1L
        );
        
        assertEquals(response1, response2);
    }

    @Test
    void testAuthTokenResponseToString() {
        AuthTokenResponse response = new AuthTokenResponse(
            "token",
            "Bearer",
            3600L,
            "test@example.com",
            "John",
            "Doe",
            1L
        );
        
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AuthTokenResponse"));
    }
}
