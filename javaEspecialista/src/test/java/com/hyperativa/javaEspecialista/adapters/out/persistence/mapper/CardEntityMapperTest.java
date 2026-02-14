package com.hyperativa.javaEspecialista.adapters.out.persistence.mapper;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.CardEntity;
import com.hyperativa.javaEspecialista.domain.model.Card;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardEntityMapperTest {

    private final CardEntityMapper mapper = new CardEntityMapper();

    @Test
    void toEntity_ShouldMapCorrectly() {
        UUID uuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Card card = new Card(uuid, "hash".getBytes(), "encrypted".getBytes(), "iv".getBytes(), "tag".getBytes(), now);

        CardEntity entity = mapper.toEntity(card);

        assertNull(entity.getId());
        assertEquals(uuid.toString(), entity.getUuid());
        assertArrayEquals(card.getCardHash(), entity.getCardHash());
        assertArrayEquals(card.getEncryptedCard(), entity.getEncryptedCard());
        assertArrayEquals(card.getEncryptionIv(), entity.getEncryptionIv());
        assertArrayEquals(card.getEncryptionTag(), entity.getEncryptionTag());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void toDomain_ShouldMapCorrectly() {
        UUID uuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CardEntity entity = new CardEntity(1L, uuid.toString(), "hash".getBytes(), "encrypted".getBytes(),
                "iv".getBytes(), "tag".getBytes(), now);

        Card domain = mapper.toDomain(entity);

        assertEquals(uuid, domain.getUuid());
        assertArrayEquals(entity.getCardHash(), domain.getCardHash());
        assertArrayEquals(entity.getEncryptedCard(), domain.getEncryptedCard());
        assertArrayEquals(entity.getEncryptionIv(), domain.getEncryptionIv());
        assertArrayEquals(entity.getEncryptionTag(), domain.getEncryptionTag());
        assertEquals(now, domain.getCreatedAt());
    }
}
