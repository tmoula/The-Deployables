package com.outreach.auth.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void testLoginRequestConstructor() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        assertEquals("test@example.com", request.email());
        assertEquals("password123", request.password());
    }

    @Test
    void testLoginRequestEquality() {
        LoginRequest request1 = new LoginRequest("test@example.com", "password123");
        LoginRequest request2 = new LoginRequest("test@example.com", "password123");
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testLoginRequestWithNullValues() {
        LoginRequest request = new LoginRequest(null, null);
        
        assertNull(request.email());
        assertNull(request.password());
    }
}
