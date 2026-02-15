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
import com.hyperativa.javaEspecialista.domain.exception.DuplicateCardException;
import com.hyperativa.javaEspecialista.domain.exception.CardValidationException;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyperativa.javaEspecialista.domain.model.Card;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CryptoPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;

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

    @Mock
    private SecurityPort securityPort;

    @InjectMocks
    private CardService cardService;

    private static final String USER = "user@example.com";
    private static final String IP = "127.0.0.1";

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
        when(securityPort.getCurrentUser()).thenReturn(USER);
        when(securityPort.getCurrentIp()).thenReturn(IP);

        // Act
        UUID result = cardService.registerCard(VALID_CARD_NUMBER);

        // Assert
        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
        verify(metricsService).incrementCardsCreated();
        verify(metricsService, never()).incrementCardsAlreadyExists();
        verify(auditPort).log(eq(USER), eq("CARD_REGISTERED"), any(), eq(IP), eq("SUCCESS"), any());
    }

    @Test
    void registerCard_WhenCardAlreadyExists_ShouldThrowDuplicateCardException() {
        // Arrange
        UUID existingUuid = UUID.randomUUID();
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.of(existingUuid));

        // Act & Assert
        assertThrows(DuplicateCardException.class,
                () -> cardService.registerCard(VALID_CARD_NUMBER));

        verify(cardRepository, never()).save(any(Card.class));
        verify(metricsService, never()).incrementCardsCreated();
        verify(metricsService).incrementCardsAlreadyExists();
    }

    @Test
    void registerCard_WhenInvalidCardNumber_ShouldThrowException() {
        // Act & Assert
        assertThrows(CardValidationException.class,
                () -> cardService.registerCard("invalid"));
        verifyNoInteractions(cardRepository, cryptoPort, metricsService);
    }

    @Test
    void findCardUuid_WhenCardExists_ShouldReturnUuid() {
        // Arrange
        UUID existingUuid = UUID.randomUUID();
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.of(existingUuid));
        when(securityPort.getCurrentUser()).thenReturn(USER);
        when(securityPort.getCurrentIp()).thenReturn(IP);

        // Act
        Optional<UUID> result = cardService.findCardUuid(VALID_CARD_NUMBER);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existingUuid, result.get());
        verify(metricsService).recordCardLookup(true);
        verify(auditPort).log(eq(USER), eq("CARD_LOOKUP"), eq(existingUuid.toString()), eq(IP), eq("SUCCESS"), any());
    }

    @Test
    void findCardUuid_WhenCardDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.findUuidByHash(HASH)).thenReturn(Optional.empty());
        when(securityPort.getCurrentUser()).thenReturn(USER);
        when(securityPort.getCurrentIp()).thenReturn(IP);

        // Act
        Optional<UUID> result = cardService.findCardUuid(VALID_CARD_NUMBER);

        // Assert
        assertFalse(result.isPresent());
        verify(metricsService).recordCardLookup(false);
        verify(auditPort).log(eq(USER), eq("CARD_LOOKUP"), any(), eq(IP), eq("NOT_FOUND"), any());
    }

    @Test
    void deleteCard_WhenCardExists_ShouldReturnTrue() {
        // Arrange
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.deleteByHash(HASH)).thenReturn(true);
        when(securityPort.getCurrentUser()).thenReturn(USER);
        when(securityPort.getCurrentIp()).thenReturn(IP);

        // Act
        boolean result = cardService.deleteCard(VALID_CARD_NUMBER);

        // Assert
        assertTrue(result);
        verify(auditPort).log(eq(USER), eq("CARD_DELETED"), any(), eq(IP), eq("SUCCESS"), any());
    }

    @Test
    void deleteCard_WhenCardDoesNotExist_ShouldReturnFalse() {
        // Arrange
        when(cryptoPort.hash(VALID_CARD_NUMBER)).thenReturn(HASH);
        when(cardRepository.deleteByHash(HASH)).thenReturn(false);
        when(securityPort.getCurrentUser()).thenReturn(USER);
        when(securityPort.getCurrentIp()).thenReturn(IP);

        // Act
        boolean result = cardService.deleteCard(VALID_CARD_NUMBER);

        // Assert
        assertFalse(result);
        verify(auditPort).log(eq(USER), eq("CARD_DELETED"), any(), eq(IP), eq("NOT_FOUND"), any());
    }
}
