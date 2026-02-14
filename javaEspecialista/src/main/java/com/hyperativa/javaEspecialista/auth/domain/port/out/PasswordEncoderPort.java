package com.hyperativa.javaEspecialista.auth.domain.port.out;

/**
 * Domain port for password encoding operations.
 * Keeps the domain layer free from Spring Security dependencies.
 */
public interface PasswordEncoderPort {

    /**
     * Encodes a raw password.
     */
    String encode(String rawPassword);

    /**
     * Verifies a raw password against an encoded one.
     */
    boolean matches(String rawPassword, String encodedPassword);
}
