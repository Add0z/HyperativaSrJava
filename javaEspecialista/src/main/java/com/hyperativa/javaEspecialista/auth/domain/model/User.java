package com.hyperativa.javaEspecialista.auth.domain.model;

import java.util.Set;
import java.util.UUID;

public record User(
        UUID id,
        String username,
        String password,
        Set<Role> roles) {
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}
