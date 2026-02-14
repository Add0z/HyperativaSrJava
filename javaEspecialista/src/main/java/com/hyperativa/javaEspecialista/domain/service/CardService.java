package com.hyperativa.javaEspecialista.domain.service;

import com.hyperativa.javaEspecialista.domain.exception.DuplicateCardException;
import com.hyperativa.javaEspecialista.domain.model.Card;
import com.hyperativa.javaEspecialista.domain.ports.in.CardInputPort;
import com.hyperativa.javaEspecialista.domain.ports.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CryptoPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * PCI DSS-compliant card tokenization service.
 * <p>
 * Replaces sensitive PAN (Primary Account Number) data with non-reversible
 * tokens (UUIDs),
 * while storing the encrypted PAN for authorized decryption when needed.
 * All operations are audited per PCI DSS Requirement 10.
 * </p>
 */
public class CardService implements CardInputPort {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepositoryPort cardRepository;
    private final CryptoPort cryptoPort;
    private final MetricsPort metricsService;
    private final AuditPort auditPort;

    public CardService(CardRepositoryPort cardRepository, CryptoPort cryptoPort,
            MetricsPort metricsService, AuditPort auditPort) {
        this.cardRepository = cardRepository;
        this.cryptoPort = cryptoPort;
        this.metricsService = metricsService;
        this.auditPort = auditPort;
    }

    public UUID registerCard(String cardNumber) {
        log.debug("Starting card registration process");
        Card.validate(cardNumber);

        byte[] cardHash = cryptoPort.hash(cardNumber);
        Optional<UUID> existingUuid = cardRepository.findUuidByHash(cardHash);

        if (existingUuid.isPresent()) {
            log.info("Card already exists with Token: {}", existingUuid.get());
            metricsService.incrementCardsAlreadyExists();
            throw new DuplicateCardException(
                    "Card already registered with Token: " + existingUuid.get());
        }
        log.debug("Card is new, proceeding with encryption and saving");

        byte[] iv = cryptoPort.generateIv();
        byte[] encryptedWithTag = cryptoPort.encrypt(cardNumber, iv);

        int tagLength = 16;
        int cipherTextLength = encryptedWithTag.length - tagLength;

        byte[] encryptedCard = Arrays.copyOfRange(encryptedWithTag, 0, cipherTextLength);
        byte[] encryptionTag = Arrays.copyOfRange(encryptedWithTag, cipherTextLength, encryptedWithTag.length);

        Card newCard = new Card(
                UUID.randomUUID(),
                cardHash,
                encryptedCard,
                iv,
                encryptionTag,
                LocalDateTime.now());

        cardRepository.save(newCard);
        metricsService.incrementCardsCreated();
        auditPort.log("system", "CARD_REGISTERED", newCard.getUuid().toString(), null, "SUCCESS", null);
        log.info("New card tokenized successfully with token: {}", newCard.getUuid());
        return newCard.getUuid();
    }

    public Optional<UUID> findCardUuid(String cardNumber) {

        byte[] cardHash = cryptoPort.hash(cardNumber);
        Optional<UUID> result = cardRepository.findUuidByHash(cardHash);
        if (result.isPresent()) {
            log.debug("Card found via hash lookup, token: {}", result.get());
            auditPort.log("system", "CARD_LOOKUP", result.get().toString(), null, "SUCCESS", null);
        } else {
            log.debug("Card not found via hash lookup");
            auditPort.log("system", "CARD_LOOKUP", null, null, "NOT_FOUND", null);
        }
        metricsService.recordCardLookup(result.isPresent());
        return result;
    }

    @Override
    public boolean deleteCard(String cardNumber) {
        log.debug("Processing card deletion request (LGPD Art. 18)");
        byte[] cardHash = cryptoPort.hash(cardNumber);
        boolean deleted = cardRepository.deleteByHash(cardHash);
        if (deleted) {
            auditPort.log("system", "CARD_DELETED", null, null, "SUCCESS", "LGPD erasure request");
            log.info("Card data erased per LGPD Art. 18 request");
        } else {
            auditPort.log("system", "CARD_DELETED", null, null, "NOT_FOUND", "LGPD erasure request - card not found");
            log.debug("Card not found for deletion");
        }
        return deleted;
    }
}
