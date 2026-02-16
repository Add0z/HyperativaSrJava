package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenEntityTest {

    @Test
    void testNoArgsConstructor() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        assertTrue(entity.isNew());
        assertNull(entity.getId());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        RefreshTokenEntity entity = new RefreshTokenEntity("id-123", "token-abc", "user-123", now, false);

        assertEquals("id-123", entity.getId());
        assertEquals("token-abc", entity.getToken());
        assertEquals("user-123", entity.getUserId());
        assertEquals(now, entity.getExpiryDate());
        assertFalse(entity.isRevoked());
        assertTrue(entity.isNew()); // Default isNew is true even with constructor unless set otherwise
    }

    @Test
    void testSetters() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        Instant now = Instant.now();

        entity.setId("id-456");
        entity.setToken("token-def");
        entity.setUserId("user-456");
        entity.setExpiryDate(now);
        entity.setRevoked(true);
        entity.setNew(false);

        assertEquals("id-456", entity.getId());
        assertEquals("token-def", entity.getToken());
        assertEquals("user-456", entity.getUserId());
        assertEquals(now, entity.getExpiryDate());
        assertTrue(entity.isRevoked());
        assertFalse(entity.isNew());
    }

    @Test
    void testIsNew() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        assertTrue(entity.isNew());

        entity.setNew(false);
        assertFalse(entity.isNew());
    }
}
