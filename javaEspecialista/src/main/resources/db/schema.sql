CREATE DATABASE IF NOT EXISTS card_registry CHARACTER
SET
    utf8mb4 COLLATE utf8mb4_unicode_ci;

USE card_registry;

CREATE TABLE cards ( id BIGINT NOT NULL AUTO_INCREMENT,

-- Identificador interno do sistema
uuid CHAR(36) NOT NULL,

-- Hash determinístico do cartão (HMAC-SHA-256)
card_hash BINARY (32) NOT NULL,

-- Cartão criptografado (AES-256-GCM)
encrypted_card BLOB NOT NULL,

-- Vetor de inicialização do AES-GCM
encryption_iv BINARY (12) NOT NULL,

-- Tag de autenticação do AES-GCM
encryption_tag BINARY (16) NOT NULL,

-- Auditoria
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (id),

-- Garantia de idempotência e unicidade
CONSTRAINT uk_cards_card_hash UNIQUE (card_hash),

-- UUID nunca se repete
CONSTRAINT uk_cards_uuid UNIQUE (uuid) ) ENGINE=InnoDB;

CREATE INDEX idx_cards_created_at ON cards (created_at);