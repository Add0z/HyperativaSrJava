package com.hyperativa.javaEspecialista.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.CardEntity;
import com.hyperativa.javaEspecialista.adapters.out.persistence.mapper.CardEntityMapper;
import com.hyperativa.javaEspecialista.adapters.out.persistence.repo.CardRepository;
import com.hyperativa.javaEspecialista.domain.model.Card;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@ExtendWith(MockitoExtension.class)
class CardRepositoryAdapterTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CardEntityMapper cardEntityMapper;

    @Mock
    private MetricsPort metricsService;

    private CardRepositoryAdapter adapter;

    private static final byte[] HASH = new byte[] { 1, 2, 3 };
    private static final String HEX_HASH = "010203";
    private static final String CACHE_KEY = "card:exists:" + HEX_HASH;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new CardRepositoryAdapter(
                cardRepository,
                redisTemplate,
                Duration.ofHours(24),
                Duration.ofMinutes(10),
                cardEntityMapper,
                metricsService);
    }

    @Test
    void save_ShouldSaveToDbAndUpdateCache() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        Card card = mock(Card.class);
        when(card.getCardHash()).thenReturn(HASH);
        when(card.getUuid()).thenReturn(uuid);

        CardEntity entity = new CardEntity();
        when(cardEntityMapper.toEntity(card)).thenReturn(entity);
        when(cardRepository.save(entity)).thenReturn(entity);
        when(cardEntityMapper.toDomain(entity)).thenReturn(card);

        // Act
        Card result = adapter.save(card);

        // Assert
        assertNotNull(result);
        verify(cardRepository).save(entity);
        verify(valueOperations).set(eq(CACHE_KEY), eq(uuid.toString()), any(Duration.class));
        verify(metricsService).recordCachePutLatency(any(Duration.class));
    }

    @Test
    void findUuidByHash_WhenInCache_ShouldReturnUuid() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        when(valueOperations.get(CACHE_KEY)).thenReturn(uuid.toString());

        // Act
        Optional<UUID> result = adapter.findUuidByHash(HASH);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
        verify(metricsService).incrementCacheHit();
        verifyNoInteractions(cardRepository);
    }

    @Test
    void findUuidByHash_WhenInCacheAsNotFound_ShouldReturnEmpty() {
        // Arrange
        when(valueOperations.get(CACHE_KEY)).thenReturn("NOT_FOUND");

        // Act
        Optional<UUID> result = adapter.findUuidByHash(HASH);

        // Assert
        assertTrue(result.isEmpty());
        verify(metricsService).incrementCacheHit();
        verifyNoInteractions(cardRepository);
    }

    @Test
    void findUuidByHash_WhenNotInCacheButInDb_ShouldReturnUuidAndUpdateCache() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);

        CardEntity entity = new CardEntity();
        entity.setUuid(uuid.toString());
        when(cardRepository.findByCardHash(HASH)).thenReturn(Optional.of(entity));

        // Act
        Optional<UUID> result = adapter.findUuidByHash(HASH);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
        verify(metricsService).incrementCacheMiss();
        verify(valueOperations).set(eq(CACHE_KEY), eq(uuid.toString()), any(Duration.class));
    }

    @Test
    void findUuidByHash_WhenNotInCacheAndNotInDb_ShouldReturnEmptyAndCacheNotFound() {
        // Arrange
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(cardRepository.findByCardHash(HASH)).thenReturn(Optional.empty());

        // Act
        Optional<UUID> result = adapter.findUuidByHash(HASH);

        // Assert
        assertTrue(result.isEmpty());
        verify(metricsService).incrementCacheMiss();
        verify(valueOperations).set(eq(CACHE_KEY), eq("NOT_FOUND"), any(Duration.class));
    }
}
