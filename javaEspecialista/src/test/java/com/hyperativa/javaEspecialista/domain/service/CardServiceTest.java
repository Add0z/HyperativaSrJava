package com.hyperativa.javaEspecialista.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyperativa.javaEspecialista.domain.model.Card;
import com.hyperativa.javaEspecialista.domain.ports.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CryptoPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepositoryPort cardRepository;

    @Mock
    private CryptoPort cryptoPort;

    @Mock
    private MetricsPort metricsService;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private CardService cardService;

    private static final String VALID_CARD_NUMBER = "1234567890123452";
    private static final byte[] HASH = "hash".getBytes();
    private static final byte[] IV = "iv12345678901234".getBytes();
    private static final byte[] ENCRYPTED_WITH_TAG = "encrypted_with_tag_16_bytes_tag".getBytes();

    @Test
    void registerCard_WhenNewCard_ShouldSaveAndReturnUuid() {
        // Arrange
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.empty());
        when(cryptoPort.generateIv()).thenReturn(IV);
        when(cryptoPort.encrypt(eq(VALID_CARD_NUMBER), eq(IV))).thenReturn(ENCRYPTED_WITH_TAG);

        // Act
        UUID result = cardService.registerCard(VALID_CARD_NUMBER);

        // Assert
        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
        verify(metricsService).incrementCardsCreated();
        verify(metricsService, never()).incrementCardsAlreadyExists();
    }

    @Test
    void registerCard_WhenCardAlreadyExists_ShouldReturnExistingUuid() {
        // Arrange
        UUID existingUuid = UUID.randomUUID();
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.of(existingUuid));

        // Act
        UUID result = cardService.registerCard(VALID_CARD_NUMBER);

        // Assert
        assertEquals(existingUuid, result);
        verify(cardRepository, never()).save(any(Card.class));
        verify(metricsService, never()).incrementCardsCreated();
        verify(metricsService).incrementCardsAlreadyExists();
    }

    @Test
    void registerCard_WhenInvalidCardNumber_ShouldThrowException() {
        // Act & Assert
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class, () -> cardService.registerCard("invalid"));
        verifyNoInteractions(cardRepository, cryptoPort, metricsService);
    }

    @Test
    void findCardUuid_WhenCardExists_ShouldReturnUuid() {
        // Arrange
        UUID existingUuid = UUID.randomUUID();
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.of(existingUuid));

        // Act
        Optional<UUID> result = cardService.findCardUuid(VALID_CARD_NUMBER);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existingUuid, result.get());
        verify(metricsService).recordCardLookup(true);
    }

    @Test
    void findCardUuid_WhenCardDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.empty());

        // Act
        Optional<UUID> result = cardService.findCardUuid(VALID_CARD_NUMBER);

        // Assert
        assertFalse(result.isPresent());
        verify(metricsService).recordCardLookup(false);
    }
}
