package com.outreach.auth.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void testRegisterRequestConstructor() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "password123",
            "John",
            "Doe"
        );
        
        assertEquals("test@example.com", request.email());
        assertEquals("password123", request.password());
        assertEquals("John", request.firstName());
        assertEquals("Doe", request.lastName());
    }

    @Test
    void testRegisterRequestEquality() {
        RegisterRequest request1 = new RegisterRequest(
            "test@example.com",
            "password123",
            "John",
            "Doe"
        );
        
        RegisterRequest request2 = new RegisterRequest(
            "test@example.com",
            "password123",
            "John",
            "Doe"
        );
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testRegisterRequestWithNullValues() {
        RegisterRequest request = new RegisterRequest(null, null, null, null);
        
        assertNull(request.email());
        assertNull(request.password());
        assertNull(request.firstName());
        assertNull(request.lastName());
    }
}
