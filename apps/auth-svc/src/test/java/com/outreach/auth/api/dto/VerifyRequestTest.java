package com.outreach.auth.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyRequestTest {

    @Test
    void testVerifyRequestConstructor() {
        VerifyRequest request = new VerifyRequest("test@example.com", "123456");
        
        assertEquals("test@example.com", request.email());
        assertEquals("123456", request.code());
    }

    @Test
    void testVerifyRequestEquality() {
        VerifyRequest request1 = new VerifyRequest("test@example.com", "123456");
        VerifyRequest request2 = new VerifyRequest("test@example.com", "123456");
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testVerifyRequestWithNullValues() {
        VerifyRequest request = new VerifyRequest(null, null);
        
        assertNull(request.email());
        assertNull(request.code());
    }
}
