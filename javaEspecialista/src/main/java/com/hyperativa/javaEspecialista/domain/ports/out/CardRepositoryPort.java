package com.hyperativa.javaEspecialista.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.hyperativa.javaEspecialista.domain.model.Card;

public interface CardRepositoryPort {
    Card save(Card card);

    Optional<UUID> findUuidByHash(byte[] cardHash);

    /**
     * Deletes a card by its hash. Required for LGPD Art. 18 (right to erasure).
     * 
     * @return true if a card was deleted, false if not found.
     */
    boolean deleteByHash(byte[] cardHash);

    /**
     * Deletes all cards whose {@code expires_at} timestamp has passed.
     * Used by the scheduled data retention cleanup task.
     *
     * @return the number of expired cards deleted.
     */
    int deleteExpiredCards();
}
