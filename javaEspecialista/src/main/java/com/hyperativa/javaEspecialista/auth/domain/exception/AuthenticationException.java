package com.hyperativa.javaEspecialista.auth.domain.exception;

/**
 * Domain exception for authentication failures.
 * Replaces Spring Security's BadCredentialsException in the domain layer,
 * keeping the domain free from framework dependencies.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
