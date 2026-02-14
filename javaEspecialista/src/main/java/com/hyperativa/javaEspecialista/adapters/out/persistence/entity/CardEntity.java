package com.hyperativa.javaEspecialista.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("cards")
public class CardEntity {

    @Id
    private Long id;
    private String uuid;
    private byte[] cardHash;
    private byte[] encryptedCard;
    private byte[] encryptionIv;
    private byte[] encryptionTag;
    private LocalDateTime createdAt;

    public CardEntity() {
    }

    public CardEntity(Long id, String uuid, byte[] cardHash, byte[] encryptedCard, byte[] encryptionIv,
            byte[] encryptionTag, LocalDateTime createdAt) {
        this.id = id;
        this.uuid = uuid;
        this.cardHash = cardHash;
        this.encryptedCard = encryptedCard;
        this.encryptionIv = encryptionIv;
        this.encryptionTag = encryptionTag;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public byte[] getCardHash() {
        return cardHash;
    }

    public void setCardHash(byte[] cardHash) {
        this.cardHash = cardHash;
    }

    public byte[] getEncryptedCard() {
        return encryptedCard;
    }

    public void setEncryptedCard(byte[] encryptedCard) {
        this.encryptedCard = encryptedCard;
    }

    public byte[] getEncryptionIv() {
        return encryptionIv;
    }

    public void setEncryptionIv(byte[] encryptionIv) {
        this.encryptionIv = encryptionIv;
    }

    public byte[] getEncryptionTag() {
        return encryptionTag;
    }

    public void setEncryptionTag(byte[] encryptionTag) {
        this.encryptionTag = encryptionTag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
