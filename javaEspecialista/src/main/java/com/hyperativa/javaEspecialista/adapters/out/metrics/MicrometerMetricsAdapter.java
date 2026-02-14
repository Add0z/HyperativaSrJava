package com.hyperativa.javaEspecialista.adapters.out.metrics;

import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Micrometer implementation of the domain MetricsPort.
 * All Micrometer/framework concerns live here in the adapter layer.
 */
@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry meterRegistry;
    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricsAdapter.class);

    // Business Metrics
    private final Counter cardsRegisteredCounter;
    private final Counter cardsAlreadyExistsCounter;
    private final Counter cardsValidationFailedCounter;
    private final Counter cardLookupTotal;
    private final Counter cardLookupFoundCounter;
    private final Counter cardLookupNotFoundCounter;

    // Cache Metrics
    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;
    private final Timer cacheGetLatencyTimer;
    private final Timer cachePutLatencyTimer;

    // Crypto Metrics
    private final Timer cryptoEncryptLatencyTimer;
    private final Counter cryptoFailuresCounter;

    // Auth Metrics
    private final Counter loginSuccessCounter;
    private final Counter userRegisteredCounter;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.cardsRegisteredCounter = Counter.builder("cards_registered_total")
                .description("Total number of cards successfully registered")
                .register(meterRegistry);

        this.cardsAlreadyExistsCounter = Counter.builder("cards_already_exists_total")
                .description("Total number of attempts to create existing cards")
                .register(meterRegistry);

        this.cardsValidationFailedCounter = Counter.builder("cards_validation_failed_total")
                .description("Total number of card validation failures")
                .register(meterRegistry);

        this.cardLookupTotal = Counter.builder("card_lookup_total")
                .description("Total number of card lookups")
                .register(meterRegistry);

        this.cardLookupFoundCounter = Counter.builder("card_lookup_found_total")
                .description("Total number of successful card lookups")
                .register(meterRegistry);

        this.cardLookupNotFoundCounter = Counter.builder("card_lookup_not_found_total")
                .description("Total number of failed card lookups")
                .register(meterRegistry);

        this.cacheHitsCounter = Counter.builder("cache_hits_total")
                .description("Total number of cache hits")
                .register(meterRegistry);

        this.cacheMissesCounter = Counter.builder("cache_misses_total")
                .description("Total number of cache misses")
                .register(meterRegistry);

        this.cacheGetLatencyTimer = Timer.builder("cache_get_latency_seconds")
                .description("Latency of cache GET operations")
                .register(meterRegistry);

        this.cachePutLatencyTimer = Timer.builder("cache_put_latency_seconds")
                .description("Latency of cache PUT operations")
                .register(meterRegistry);

        this.cryptoEncryptLatencyTimer = Timer.builder("crypto_encrypt_latency_seconds")
                .description("Latency of encryption operations")
                .register(meterRegistry);

        this.cryptoFailuresCounter = Counter.builder("crypto_failures_total")
                .description("Total number of cryptographic failures")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("auth_login_success_total")
                .description("Total number of successful logins")
                .register(meterRegistry);

        this.userRegisteredCounter = Counter.builder("auth_user_registered_total")
                .description("Total number of users successfully registered")
                .register(meterRegistry);
    }

    @Override
    public void incrementCardsCreated() {
        log.debug("Incrementing cards_registered_total metric");
        cardsRegisteredCounter.increment();
    }

    @Override
    public void incrementCardsAlreadyExists() {
        log.debug("Incrementing cards_already_exists_total metric");
        cardsAlreadyExistsCounter.increment();
    }

    @Override
    public void incrementCardsValidationFailed() {
        cardsValidationFailedCounter.increment();
    }

    @Override
    public void recordCardLookup(boolean found) {
        cardLookupTotal.increment();
        if (found) {
            cardLookupFoundCounter.increment();
        } else {
            cardLookupNotFoundCounter.increment();
        }
    }

    @Override
    public void incrementCacheHit() {
        cacheHitsCounter.increment();
    }

    @Override
    public void incrementCacheMiss() {
        cacheMissesCounter.increment();
    }

    @Override
    public void recordCacheGetLatency(Duration duration) {
        cacheGetLatencyTimer.record(duration);
    }

    @Override
    public void recordCachePutLatency(Duration duration) {
        cachePutLatencyTimer.record(duration);
    }

    @Override
    public void incrementRateLimit(boolean allowed, String clientId, String endpoint) {
        String counterName = allowed ? "rate_limit_allowed_total" : "rate_limit_blocked_total";
        Counter.builder(counterName)
                .tag("client_id", clientId != null ? clientId : "unknown")
                .tag("endpoint", endpoint != null ? endpoint : "unknown")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordCryptoEncryptLatency(Duration duration) {
        cryptoEncryptLatencyTimer.record(duration);
    }

    @Override
    public void incrementCryptoFailure() {
        log.error("Incrementing crypto_failures_total metric");
        cryptoFailuresCounter.increment();
    }

    @Override
    public void incrementLoginSuccess() {
        log.debug("Incrementing auth_login_success_total metric");
        loginSuccessCounter.increment();
    }

    @Override
    public void incrementLoginFailure(String reason) {
        Counter.builder("auth_login_failure_total")
                .description("Total number of failed logins")
                .tag("reason", reason != null ? reason : "unknown")
                .register(meterRegistry)
                .increment();
        log.warn("Incrementing auth_login_failure_total metric. Reason: {}", reason);
    }

    @Override
    public void incrementUserRegistered(String role) {
        userRegisteredCounter.increment();
        Counter.builder("auth_user_registered_by_role_total")
                .description("Total number of users registered by role")
                .tag("role", role != null ? role : "unknown")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void incrementUserRegistrationFailure(String reason) {
        Counter.builder("auth_user_registration_failed_total")
                .description("Total number of failed user registrations")
                .tag("reason", reason != null ? reason : "unknown")
                .register(meterRegistry)
                .increment();
    }
}
