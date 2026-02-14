package com.hyperativa.javaEspecialista.adapters.out.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class CardEntityTest {

    @Test
    void testCardEntityGettersAndSetters() {
        CardEntity entity = new CardEntity();
        LocalDateTime now = LocalDateTime.now();
        byte[] hash = "hash".getBytes();
        byte[] encrypted = "encrypted".getBytes();
        byte[] iv = "iv".getBytes();
        byte[] tag = "tag".getBytes();

        entity.setId(1L);
        entity.setUuid("uuid");
        entity.setCardHash(hash);
        entity.setEncryptedCard(encrypted);
        entity.setEncryptionIv(iv);
        entity.setEncryptionTag(tag);
        entity.setCreatedAt(now);

        assertEquals(1L, entity.getId());
        assertEquals("uuid", entity.getUuid());
        assertArrayEquals(hash, entity.getCardHash());
        assertArrayEquals(encrypted, entity.getEncryptedCard());
        assertArrayEquals(iv, entity.getEncryptionIv());
        assertArrayEquals(tag, entity.getEncryptionTag());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void testCardEntityFullConstructor() {
        LocalDateTime now = LocalDateTime.now();
        byte[] hash = "hash".getBytes();
        byte[] encrypted = "encrypted".getBytes();
        byte[] iv = "iv".getBytes();
        byte[] tag = "tag".getBytes();

        CardEntity entity = new CardEntity(1L, "uuid", hash, encrypted, iv, tag, now);

        assertEquals(1L, entity.getId());
        assertEquals("uuid", entity.getUuid());
        assertArrayEquals(hash, entity.getCardHash());
        assertArrayEquals(encrypted, entity.getEncryptedCard());
        assertArrayEquals(iv, entity.getEncryptionIv());
        assertArrayEquals(tag, entity.getEncryptionTag());
        assertEquals(now, entity.getCreatedAt());
    }
}
