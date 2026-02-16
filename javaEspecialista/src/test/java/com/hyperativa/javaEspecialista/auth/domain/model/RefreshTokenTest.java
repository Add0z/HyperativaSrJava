package com.hyperativa.javaEspecialista.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void constructor_WhenTokenIsValid_ShouldCreateRefreshToken() {
        UUID id = UUID.randomUUID();
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        Instant expiryDate = Instant.now().plus(1, ChronoUnit.HOURS);

        RefreshToken refreshToken = new RefreshToken(id, token, userId, expiryDate, false);

        assertEquals(id, refreshToken.id());
        assertEquals(token, refreshToken.token());
        assertEquals(userId, refreshToken.userId());
        assertEquals(expiryDate, refreshToken.expiryDate());
        assertFalse(refreshToken.revoked());
    }

    @Test
    void constructor_WhenTokenIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshToken(UUID.randomUUID(), null, UUID.randomUUID(), Instant.now(), false));
    }

    @Test
    void constructor_WhenTokenIsEmpty_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshToken(UUID.randomUUID(), "", UUID.randomUUID(), Instant.now(), false));
    }

    @Test
    void isExpired_WhenDateIsInPast_ShouldReturnTrue() {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(), "token", UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.HOURS), false);
        assertTrue(refreshToken.isExpired());
    }

    @Test
    void isExpired_WhenDateIsInFuture_ShouldReturnFalse() {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(), "token", UUID.randomUUID(),
                Instant.now().plus(1, ChronoUnit.HOURS), false);
        assertFalse(refreshToken.isExpired());
    }

    @Test
    void isValid_WhenNotRevokedAndNotExpired_ShouldReturnTrue() {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(), "token", UUID.randomUUID(),
                Instant.now().plus(1, ChronoUnit.HOURS), false);
        assertTrue(refreshToken.isValid());
    }

    @Test
    void isValid_WhenRevoked_ShouldReturnFalse() {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(), "token", UUID.randomUUID(),
                Instant.now().plus(1, ChronoUnit.HOURS), true);
        assertFalse(refreshToken.isValid());
    }

    @Test
    void isValid_WhenExpired_ShouldReturnFalse() {
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(), "token", UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.HOURS), false);
        assertFalse(refreshToken.isValid());
    }
}
