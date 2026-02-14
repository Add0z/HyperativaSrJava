package com.hyperativa.javaEspecialista.auth.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private AuthService.TokenProvider tokenProvider;

    @Mock
    private MetricsPort metricsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_WhenValidCredentials_ShouldReturnToken() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "encodedPass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);
        when(tokenProvider.generateToken(user)).thenReturn("token");

        // Act
        String result = authService.login("user", "pass");

        // Assert
        assertEquals("token", result);
        verify(tokenProvider).generateToken(user);
        verify(metricsService).incrementLoginSuccess();
    }

    @Test
    void login_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login("user", "pass"));
        verify(metricsService).incrementLoginFailure("user_not_found");
    }

    @Test
    void login_WhenPasswordMismatch_ShouldThrowAuthenticationException() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "encodedPass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login("user", "wrong"));
        verify(metricsService).incrementLoginFailure("bad_credentials");
    }

    @Test
    void register_WhenValidData_ShouldSaveUser() {
        // Arrange
        when(loadUserPort.loadUserByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");

        // Act
        authService.register("newuser", "pass");

        // Assert
        verify(saveUserPort).save(argThat(u -> u.roles().contains(Role.USER)));
        verify(metricsService).incrementUserRegistered("USER");
    }

    @Test
    void register_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "pass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException.class,
                () -> authService.register("user", "pass"));
        verify(metricsService).incrementUserRegistrationFailure("username_exists");
    }
}
