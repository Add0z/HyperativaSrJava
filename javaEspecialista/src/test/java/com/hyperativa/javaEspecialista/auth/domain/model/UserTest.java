package com.hyperativa.javaEspecialista.auth.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWhenValid() {
        UUID id = UUID.randomUUID();
        String username = "testuser";
        String password = "password";
        Set<Role> roles = Set.of(Role.USER);

        User user = new User(id, username, password, roles);

        assertEquals(id, user.id());
        assertEquals(username, user.username());
        assertEquals(password, user.password());
        assertEquals(roles, user.roles());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "  \t" })
    void shouldThrowExceptionWhenUsernameIsInvalid(String username) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), username, "password", Set.of()));

        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "  \t" })
    void shouldThrowExceptionWhenPasswordIsInvalid(String password) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new User(UUID.randomUUID(), "username", password, Set.of()));

        assertEquals("Password cannot be null or empty", exception.getMessage());
    }
}
