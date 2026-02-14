package com.hyperativa.javaEspecialista.domain.model;

import com.hyperativa.javaEspecialista.domain.exception.CardValidationException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Immutable domain model for a tokenized card.
 * All byte[] fields use defensive copies in constructor and getters.
 */
public class Card {

    private final UUID uuid;
    private final byte[] cardHash; // HMAC-SHA-256
    private final byte[] encryptedCard; // AES-256-GCM ciphertext
    private final byte[] encryptionIv;
    private final byte[] encryptionTag;
    private final LocalDateTime createdAt;

    private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\d+$");

    public Card(UUID uuid, byte[] cardHash, byte[] encryptedCard, byte[] encryptionIv, byte[] encryptionTag,
            LocalDateTime createdAt) {
        this.uuid = uuid;
        this.cardHash = cardHash != null ? Arrays.copyOf(cardHash, cardHash.length) : null;
        this.encryptedCard = encryptedCard != null ? Arrays.copyOf(encryptedCard, encryptedCard.length) : null;
        this.encryptionIv = encryptionIv != null ? Arrays.copyOf(encryptionIv, encryptionIv.length) : null;
        this.encryptionTag = encryptionTag != null ? Arrays.copyOf(encryptionTag, encryptionTag.length) : null;
        this.createdAt = createdAt;
    }

    public static void validate(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new CardValidationException("Card number cannot be empty");
        }
        if (!DIGIT_PATTERN.matcher(cardNumber).matches()) {
            throw new CardValidationException("Card number must contain only digits");
        }
        if (!isLuhnValid(cardNumber)) {
            throw new CardValidationException("Invalid card number (Luhn check failed)");
        }
    }

    private static boolean isLuhnValid(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public UUID getUuid() {
        return uuid;
    }

    public byte[] getCardHash() {
        return cardHash != null ? Arrays.copyOf(cardHash, cardHash.length) : null;
    }

    public byte[] getEncryptedCard() {
        return encryptedCard != null ? Arrays.copyOf(encryptedCard, encryptedCard.length) : null;
    }

    public byte[] getEncryptionIv() {
        return encryptionIv != null ? Arrays.copyOf(encryptionIv, encryptionIv.length) : null;
    }

    public byte[] getEncryptionTag() {
        return encryptionTag != null ? Arrays.copyOf(encryptionTag, encryptionTag.length) : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
