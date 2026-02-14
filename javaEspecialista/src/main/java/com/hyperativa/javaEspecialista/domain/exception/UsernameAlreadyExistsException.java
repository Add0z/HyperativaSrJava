package com.hyperativa.javaEspecialista.domain.exception;

/**
 * Thrown when a username already exists during registration.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
