package com.hyperativa.javaEspecialista.adapters.out.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.CardEntity;
import com.hyperativa.javaEspecialista.adapters.out.persistence.repo.CardRepository;
import com.hyperativa.javaEspecialista.domain.model.Card;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ENCRYPTION_KEY=12345678901234567890123456789012",
        "HASH_KEY=12345678901234567890123456789012",
        "JWT_PUBLIC_KEY=classpath:public.pem",
        "JWT_PRIVATE_KEY=classpath:private.pem"
})
@Testcontainers
class CardRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0").withExposedPorts(6379);

    @Autowired
    private CardRepositoryAdapter adapter;

    @Autowired
    private CardRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldSaveAndFindCard() {
        UUID uuid = UUID.randomUUID();
        byte[] hash = new byte[32];
        new java.util.Random().nextBytes(hash);
        byte[] enc = new byte[] { 4, 5, 6 };
        byte[] iv = new byte[] { 7, 8, 9 };
        byte[] tag = new byte[] { 10, 11, 12 };
        Card card = new Card(uuid, hash, enc, iv, tag, LocalDateTime.now());

        // Save
        Card saved = adapter.save(card);
        assertThat(saved).isNotNull();
        assertThat(saved.getUuid()).isEqualTo(uuid);

        // Verify MySQL
        Optional<CardEntity> entity = repository.findByCardHash(hash);
        assertThat(entity).isPresent();
        assertThat(entity.get().getUuid()).isEqualTo(uuid.toString());

        // Verify Redis Cache (Write-Through)
        // Manual check of key generation logic from Adapter: "card:exists:" + hex
        String hexHash = java.util.HexFormat.of().formatHex(hash);
        String savedUuid = redisTemplate.opsForValue().get("card:exists:" + hexHash);
        assertThat(savedUuid).isEqualTo(uuid.toString());

        // Verify Find (Hit Cache)
        Optional<UUID> foundUuid = adapter.findUuidByHash(hash);
        assertThat(foundUuid).isPresent();
        assertThat(foundUuid.get()).isEqualTo(uuid);
    }

    @Test
    void shouldCacheNegativeResult() {
        byte[] hash = new byte[32];
        new java.util.Random().nextBytes(hash);

        // Find (Miss DB)
        Optional<UUID> foundUuid = adapter.findUuidByHash(hash);
        assertThat(foundUuid).isEmpty();

        // Verify Redis Negative Cache
        String hexHash = java.util.HexFormat.of().formatHex(hash);
        String cachedValue = redisTemplate.opsForValue().get("card:exists:" + hexHash);
        assertThat(cachedValue).isEqualTo("NOT_FOUND");

        // Find again (Hit Negative Cache) - Adapter handles NOT_FOUND value
        Optional<UUID> foundUuidSecond = adapter.findUuidByHash(hash);
        assertThat(foundUuidSecond).isEmpty();
    }
}
