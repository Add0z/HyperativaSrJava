package com.hyperativa.javaEspecialista.domain.exception;

/**
 * Thrown when a cryptographic operation (encryption/hashing) fails.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
