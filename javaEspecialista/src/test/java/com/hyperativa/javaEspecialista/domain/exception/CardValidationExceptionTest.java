package com.hyperativa.javaEspecialista.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CardValidationExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Validation failed";
        CardValidationException exception = new CardValidationException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Validation failed";
        Throwable cause = new RuntimeException("Original error");
        CardValidationException exception = new CardValidationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
