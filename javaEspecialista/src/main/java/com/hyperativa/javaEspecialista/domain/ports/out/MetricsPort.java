package com.hyperativa.javaEspecialista.domain.ports.out;

import java.time.Duration;

/**
 * Domain port for application metrics.
 * Keeps the domain free from Micrometer/framework dependencies.
 */
public interface MetricsPort {

    // ── Business Metrics ──
    void incrementCardsCreated();

    void incrementCardsAlreadyExists();

    void incrementCardsValidationFailed();

    void recordCardLookup(boolean found);

    // ── Cache Metrics ──
    void incrementCacheHit();

    void incrementCacheMiss();

    void recordCacheGetLatency(Duration duration);

    void recordCachePutLatency(Duration duration);

    // ── Crypto Metrics ──
    void recordCryptoEncryptLatency(Duration duration);

    void incrementCryptoFailure();

    // ── Rate Limit Metrics ──
    void incrementRateLimit(boolean allowed, String clientId, String endpoint);

    // ── Auth Metrics ──
    void incrementLoginSuccess();

    void incrementLoginFailure(String reason);

    void incrementUserRegistered(String role);

    void incrementUserRegistrationFailure(String reason);
}
