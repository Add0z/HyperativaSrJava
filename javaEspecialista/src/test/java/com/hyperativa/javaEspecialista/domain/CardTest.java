package com.hyperativa.javaEspecialista.domain;

import org.junit.jupiter.api.Test;

import com.hyperativa.javaEspecialista.domain.model.Card;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void shouldValidateValidCardNumber() {
        // A valid Luhn number (e.g. 453201511283036)
        // 4 5 3 2 0 1 5 1 1 2 8 3 0 3 6
        // Double every second digit from right:
        // 6, 3*2=6, 0, 3*2=6, 8, 2*2=4, 1, 1*2=2, 5, 1*2=2, 0, 2*2=4, 3, 5*2=10->1+0=1,
        // 4
        // Sum: 6+6+0+6+8+4+1+2+5+2+0+4+3+1+4 = 52? Wait.
        // Let's use a known valid number or write the logic to generate one?
        // 49927398716 is valid visa.

        assertDoesNotThrow(() -> Card.validate("49927398716"));
    }

    @Test
    void shouldThrowExceptionForInvalidLuhn() {
        // 49927398717 (last digit changed)
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class, () -> Card.validate("49927398717"));
    }

    @Test
    void shouldThrowExceptionForNonDigits() {
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class, () -> Card.validate("4992739871a"));
    }

    @Test
    void shouldThrowExceptionForEmpty() {
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class, () -> Card.validate(""));
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class, () -> Card.validate(null));
    }

    @Test
    void shouldCreateCard() {
        UUID uuid = UUID.randomUUID();
        byte[] hash = new byte[] { 1, 2, 3 };
        byte[] enc = new byte[] { 4, 5 };
        byte[] iv = new byte[] { 6 };
        byte[] tag = new byte[] { 7 };
        LocalDateTime now = LocalDateTime.now();

        Card card = new Card(uuid, hash, enc, iv, tag, now);

        assertEquals(uuid, card.getUuid());
        assertArrayEquals(hash, card.getCardHash());
    }
}
