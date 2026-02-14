package com.hyperativa.javaEspecialista.adapters.out.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.CardEntity;
import com.hyperativa.javaEspecialista.adapters.out.persistence.mapper.CardEntityMapper;
import com.hyperativa.javaEspecialista.adapters.out.persistence.repo.CardRepository;
import com.hyperativa.javaEspecialista.domain.model.Card;
import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

@Component
public class CardRepositoryAdapter implements CardRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(CardRepositoryAdapter.class);
    private static final String CARD_EXISTS = "card:exists:";
    private static final String DATABASE = "database";
    private final CardRepository cardRepository;
    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtlFound;
    private final Duration cacheTtlNotFound;
    private final CardEntityMapper cardEntityMapper;
    private final MetricsPort metricsService;

    public CardRepositoryAdapter(CardRepository cardRepository,
            StringRedisTemplate redisTemplate,
            @Value("${app.cache.ttl-found:24h}") Duration cacheTtlFound,
            @Value("${app.cache.ttl-not-found:10m}") Duration cacheTtlNotFound,
            CardEntityMapper cardEntityMapper,
            MetricsPort metricsService) {
        this.cardRepository = cardRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtlFound = cacheTtlFound;
        this.cacheTtlNotFound = cacheTtlNotFound;
        this.cardEntityMapper = cardEntityMapper;
        this.metricsService = metricsService;
    }

    @Override
    @CircuitBreaker(name = DATABASE)
    @Retry(name = DATABASE)
    public Card save(Card card) {
        log.debug("Saving card to database: {}", card.getUuid());
        CardEntity entity = cardEntityMapper.toEntity(card);
        CardEntity savedEntity = Objects.requireNonNull(cardRepository.save(entity));

        // Update Cache (Read-Through / Write-Through)
        String hexHash = HexFormat.of().formatHex(card.getCardHash());
        String cacheKey = CARD_EXISTS + hexHash;

        Instant start = Instant.now();
        redisTemplate.opsForValue().set(cacheKey, Objects.requireNonNull(card.getUuid().toString()),
                Objects.requireNonNull(cacheTtlFound));
        metricsService.recordCachePutLatency(Duration.between(start, Instant.now()));
        log.debug("Card cached with key: {}", cacheKey);

        return cardEntityMapper.toDomain(savedEntity);
    }

    @Override
    @CircuitBreaker(name = DATABASE)
    @Retry(name = DATABASE)
    public Optional<UUID> findUuidByHash(byte[] cardHash) {
        String hexHash = HexFormat.of().formatHex(cardHash);
        log.debug("Finding card UUID by hash: {}", hexHash);
        String cacheKey = CARD_EXISTS + hexHash;

        Instant start = Instant.now();
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        metricsService.recordCacheGetLatency(Duration.between(start, Instant.now()));

        if (cachedValue != null) {
            metricsService.incrementCacheHit();
            if ("NOT_FOUND".equals(cachedValue)) {
                log.debug("Cache hit (negative) for key: {}", cacheKey);
                return Optional.empty();
            }
            log.debug("Cache hit for key: {}, found UUID: {}", cacheKey, cachedValue);
            return Optional.of(UUID.fromString(cachedValue));
        }

        metricsService.incrementCacheMiss();
        log.debug("Cache miss for key: {}, querying database", cacheKey);

        // Cache Miss - Query DB
        Optional<CardEntity> entityOpt = cardRepository.findByCardHash(cardHash);

        if (entityOpt.isPresent()) {
            String uuidStr = entityOpt.get().getUuid();

            Instant startPut = Instant.now();
            redisTemplate.opsForValue().set(cacheKey, Objects.requireNonNull(uuidStr),
                    Objects.requireNonNull(cacheTtlFound));
            metricsService.recordCachePutLatency(Duration.between(startPut, Instant.now()));

            return Optional.of(UUID.fromString(uuidStr));
        } else {
            log.debug("Card hash not found in database: {}", hexHash);
            // Negative Caching
            Instant startPut = Instant.now();
            redisTemplate.opsForValue().set(cacheKey, "NOT_FOUND", Objects.requireNonNull(cacheTtlNotFound));
            metricsService.recordCachePutLatency(Duration.between(startPut, Instant.now()));

            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = DATABASE)
    @Retry(name = DATABASE)
    public boolean deleteByHash(byte[] cardHash) {
        String hexHash = HexFormat.of().formatHex(cardHash);
        log.debug("Deleting card by hash: {}", hexHash);

        int deleted = cardRepository.deleteByCardHash(cardHash);

        if (deleted > 0) {
            // Invalidate cache
            String cacheKey = CARD_EXISTS + hexHash;
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.warn("Failed to invalidate cache for deleted card: {}", e.getMessage());
            }
            log.info("Card deleted successfully, hash: {}", hexHash);
            return true;
        }

        log.debug("No card found to delete with hash: {}", hexHash);
        return false;
    }

}
