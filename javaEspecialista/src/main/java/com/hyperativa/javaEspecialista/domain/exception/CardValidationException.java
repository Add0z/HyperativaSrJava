package com.hyperativa.javaEspecialista.domain.exception;

/**
 * Thrown when card number validation fails (format, Luhn check, etc.).
 */
public class CardValidationException extends RuntimeException {

    public CardValidationException(String message) {
        super(message);
    }

    public CardValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
