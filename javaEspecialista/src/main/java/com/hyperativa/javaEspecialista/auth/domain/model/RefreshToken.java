package com.hyperativa.javaEspecialista.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        String token,
        UUID userId,
        Instant expiryDate,
        boolean revoked) {
    public RefreshToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Asset Token cannot be null or empty");
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
