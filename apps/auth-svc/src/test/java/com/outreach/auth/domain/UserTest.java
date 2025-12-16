package com.outreach.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserConstructorAndGetters() {
        User user = new User("test@example.com", "password123", "John", "Doe");
        
        assertNull(user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPasswordHash());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(false, user.getIsVerified());
        assertNull(user.getVerificationCode());
        assertNotNull(user.getCreatedAt());
        assertEquals(true, user.getEnabled());
    }

    @Test
    void testUserSetters() {
        User user = new User("test@example.com", "password123", "John", "Doe");
        
        user.setId(1L);
        user.setEmail("newemail@example.com");
        user.setPasswordHash("newpassword");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setIsVerified(true);
        user.setVerificationCode("123456");
        user.setEnabled(false);
        
        assertEquals(1L, user.getId());
        assertEquals("newemail@example.com", user.getEmail());
        assertEquals("newpassword", user.getPasswordHash());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertTrue(user.getIsVerified());
        assertEquals("123456", user.getVerificationCode());
        assertFalse(user.getEnabled());
    }

    @Test
    void testUserDefaultValues() {
        User user = new User();
        
        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertEquals(true, user.getEnabled());
        assertEquals(false, user.getIsVerified());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void testUserWithNullValues() {
        User user = new User(null, null, null, null);
        
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertEquals(true, user.getEnabled());
        assertEquals(false, user.getIsVerified());
    }

    @Test
    void testVerificationCode() {
        User user = new User("test@example.com", "password123", "John", "Doe");
        
        assertNull(user.getVerificationCode());
        
        user.setVerificationCode("ABC123");
        assertEquals("ABC123", user.getVerificationCode());
    }

    @Test
    void testIsVerified() {
        User user = new User("test@example.com", "password123", "John", "Doe");
        
        assertEquals(false, user.getIsVerified());
        
        user.setIsVerified(true);
        assertEquals(true, user.getIsVerified());
    }
}
