package com.hyperativa.javaEspecialista.adapters.out.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MicrometerMetricsAdapterTest {

    private MeterRegistry meterRegistry;
    private MicrometerMetricsAdapter metricsAdapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsAdapter = new MicrometerMetricsAdapter(meterRegistry);
    }

    @Test
    void testIncrementCounters() {
        metricsAdapter.incrementCardsCreated();
        metricsAdapter.incrementCardsAlreadyExists();
        metricsAdapter.incrementCardsValidationFailed();
        metricsAdapter.incrementCacheHit();
        metricsAdapter.incrementCacheMiss();
        metricsAdapter.incrementCryptoFailure();

        assertEquals(1, meterRegistry.get("cards_registered_total").counter().count());
        assertEquals(1, meterRegistry.get("cards_already_exists_total").counter().count());
        assertEquals(1, meterRegistry.get("cards_validation_failed_total").counter().count());
        assertEquals(1, meterRegistry.get("cache_hits_total").counter().count());
        assertEquals(1, meterRegistry.get("cache_misses_total").counter().count());
        assertEquals(1, meterRegistry.get("crypto_failures_total").counter().count());
    }

    @Test
    void recordCardLookup_ShouldIncrementCorrectCounters() {
        metricsAdapter.recordCardLookup(true);
        metricsAdapter.recordCardLookup(false);

        assertEquals(2, meterRegistry.get("card_lookup_total").counter().count());
        assertEquals(1, meterRegistry.get("card_lookup_found_total").counter().count());
        assertEquals(1, meterRegistry.get("card_lookup_not_found_total").counter().count());
    }

    @Test
    void testRecordLatencies() {
        metricsAdapter.recordCacheGetLatency(Duration.ofMillis(100));
        metricsAdapter.recordCachePutLatency(Duration.ofMillis(200));
        metricsAdapter.recordCryptoEncryptLatency(Duration.ofMillis(300));

        assertEquals(0.1, meterRegistry.get("cache_get_latency_seconds").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0.2, meterRegistry.get("cache_put_latency_seconds").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0.3, meterRegistry.get("crypto_encrypt_latency_seconds").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void incrementRateLimit_ShouldCreateCounterWithTags() {
        metricsAdapter.incrementRateLimit(true, "client1", "/api/test");
        metricsAdapter.incrementRateLimit(false, "client2", "/api/test");

        assertEquals(1, meterRegistry.get("rate_limit_allowed_total").tag("client_id", "client1").counter().count());
        assertEquals(1, meterRegistry.get("rate_limit_blocked_total").tag("client_id", "client2").counter().count());
    }

    @Test
    void incrementRateLimit_WithNulls_ShouldUseDefaults() {
        metricsAdapter.incrementRateLimit(true, null, null);
        assertEquals(1, meterRegistry.get("rate_limit_allowed_total").tag("client_id", "unknown").counter().count());
    }

    @Test
    void testAuthMetrics() {
        metricsAdapter.incrementLoginSuccess();
        metricsAdapter.incrementLoginFailure("bad_credentials");
        metricsAdapter.incrementUserRegistered("ADMIN");
        metricsAdapter.incrementUserRegistrationFailure("username_exists");

        assertEquals(1, meterRegistry.get("auth_login_success_total").counter().count());
        assertEquals(1,
                meterRegistry.get("auth_login_failure_total").tag("reason", "bad_credentials").counter().count());
        assertEquals(1, meterRegistry.get("auth_user_registered_total").counter().count());
        assertEquals(1, meterRegistry.get("auth_user_registered_by_role_total").tag("role", "ADMIN").counter().count());
        assertEquals(1, meterRegistry.get("auth_user_registration_failed_total").tag("reason", "username_exists")
                .counter().count());
    }
}
