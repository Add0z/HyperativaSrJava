package com.hyperativa.javaEspecialista.auth.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationExceptionTest {

    @Test
    void shouldCreateWithCause() {
        String msg = "Error";
        Throwable cause = new RuntimeException("Cause");
        AuthenticationException ex = new AuthenticationException(msg, cause);

        assertEquals(msg, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
