package com.hyperativa.javaEspecialista.domain.ports.in;

import java.util.UUID;
import java.util.Optional;

public interface CardInputPort {
    UUID registerCard(String cardNumber);

    Optional<UUID> findCardUuid(String cardNumber);

    /**
     * Deletes a card by its number. LGPD Art. 18 - Right to erasure.
     * 
     * @return true if the card was deleted.
     */
    boolean deleteCard(String cardNumber);
}
