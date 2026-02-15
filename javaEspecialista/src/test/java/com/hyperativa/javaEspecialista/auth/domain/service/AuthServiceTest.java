package com.hyperativa.javaEspecialista.auth.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import com.hyperativa.javaEspecialista.auth.domain.model.RefreshToken;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadRefreshTokenPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveRefreshTokenPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private SaveRefreshTokenPort saveRefreshTokenPort;

    @Mock
    private LoadRefreshTokenPort loadRefreshTokenPort;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private AuthService.TokenProvider tokenProvider;

    @Mock
    private MetricsPort metricsService;

    @Mock
    private AuditPort auditPort;

    @Mock
    private SecurityPort securityPort;

    private AuthService authService; // Keeping InjectMocks might work if I verify the primitive, but manual is safer
                                     // with mixed types.
    // Actually, I'll remove InjectMocks and do it manually.

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(loadUserPort, saveUserPort, saveRefreshTokenPort, loadRefreshTokenPort,
                passwordEncoder, tokenProvider, metricsService, auditPort, securityPort, 3600L);
    }

    @Test
    void login_WhenValidCredentials_ShouldReturnToken() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "encodedPass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);
        when(tokenProvider.generateAccessToken(user)).thenReturn(new AuthService.AccessToken("access-token", 3600));
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), "refresh", user.id(), java.time.Instant.now(),
                false);
        when(saveRefreshTokenPort.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        com.hyperativa.javaEspecialista.auth.domain.model.TokenPair result = authService.login("user", "pass");

        // Assert
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        verify(tokenProvider).generateAccessToken(user);
        verify(metricsService).incrementLoginSuccess();
        verify(auditPort).log(eq("user"), eq("LOGIN_ATTEMPT"), any(), eq("127.0.0.1"), eq("PENDING"), any());
        verify(auditPort).log(eq("user"), eq("LOGIN_SUCCESS"), any(), eq("127.0.0.1"), eq("SUCCESS"), any());
    }

    @Test
    void login_WhenUserNotFound_ShouldThrowAuthenticationException() {
        // Arrange
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.empty());
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login("user", "pass"));
        verify(metricsService).incrementLoginFailure("user_not_found");
        verify(auditPort).log(eq("user"), eq("LOGIN_ATTEMPT"), any(), eq("127.0.0.1"), eq("PENDING"), any());
        verify(auditPort).log(eq("user"), eq("LOGIN_FAILURE"), any(), eq("127.0.0.1"), eq("FAILURE"),
                eq("User not found"));
    }

    @Test
    void login_WhenPasswordMismatch_ShouldThrowAuthenticationException() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "encodedPass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login("user", "wrong"));
        verify(metricsService).incrementLoginFailure("bad_credentials");
        verify(auditPort).log(eq("user"), eq("LOGIN_ATTEMPT"), any(), eq("127.0.0.1"), eq("PENDING"), any());
        verify(auditPort).log(eq("user"), eq("LOGIN_FAILURE"), any(), eq("127.0.0.1"), eq("FAILURE"),
                eq("Bad credentials"));
    }

    @Test
    void register_WhenValidData_ShouldSaveUser() {
        // Arrange
        when(loadUserPort.loadUserByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act
        authService.register("newuser", "pass", null);

        // Assert
        verify(saveUserPort).save(argThat(u -> u.roles().contains(Role.USER)));
        verify(metricsService).incrementUserRegistered("[USER]");
        verify(auditPort).log(eq("newuser"), eq("REGISTER_ATTEMPT"), any(), eq("127.0.0.1"), eq("PENDING"), any());
        verify(auditPort).log(eq("newuser"), eq("REGISTER_SUCCESS"), any(), eq("127.0.0.1"), eq("SUCCESS"), any());
    }

    @Test
    void register_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        User user = new User(UUID.randomUUID(), "user", "pass", Set.of(Role.ADMIN));
        when(loadUserPort.loadUserByUsername("user")).thenReturn(Optional.of(user));
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act & Assert
        assertThrows(com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException.class,
                () -> authService.register("user", "pass", null));
        verify(metricsService).incrementUserRegistrationFailure("username_exists");
        verify(auditPort).log(eq("user"), eq("REGISTER_ATTEMPT"), any(), eq("127.0.0.1"), eq("PENDING"), any());
        verify(auditPort).log(eq("user"), eq("REGISTER_FAILURE"), any(), eq("127.0.0.1"), eq("FAILURE"),
                eq("Username already exists"));
    }

    @Test
    void refreshToken_WhenValid_ShouldRotateTokens() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user", "pass", Set.of(Role.USER));
        RefreshToken oldToken = new RefreshToken(UUID.randomUUID(), "old-refresh", userId,
                java.time.Instant.now().plusSeconds(3600), false);

        when(loadRefreshTokenPort.loadByToken("old-refresh")).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadUserById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn(new AuthService.AccessToken("new-access", 3600));
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        RefreshToken newRefreshToken = new RefreshToken(UUID.randomUUID(), "new-refresh", userId,
                java.time.Instant.now(), false);
        when(saveRefreshTokenPort.save(any(RefreshToken.class))).thenReturn(newRefreshToken);

        // Act
        com.hyperativa.javaEspecialista.auth.domain.model.TokenPair result = authService.refreshToken("old-refresh");

        // Assert
        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());

        // Verify revocation of old token
        verify(saveRefreshTokenPort).save(argThat(t -> t.token().equals("old-refresh") && t.revoked()));
        // Verify creation of new token
        verify(saveRefreshTokenPort).save(argThat(t -> !t.token().equals("old-refresh") && !t.revoked()));
    }

    @Test
    void refreshToken_WhenRevoked_ShouldRevokeAllAndThrowException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RefreshToken revokedToken = new RefreshToken(UUID.randomUUID(), "revoked-refresh", userId,
                java.time.Instant.now().plusSeconds(3600), true);

        when(loadRefreshTokenPort.loadByToken("revoked-refresh")).thenReturn(Optional.of(revokedToken));
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("revoked-refresh"));

        verify(saveRefreshTokenPort).revokeAllUserTokens(userId);
        verify(auditPort).log(eq(userId.toString()), eq("REFRESH_TOKEN_THEFT_DETECTED"), any(), eq("127.0.0.1"),
                eq("FAILURE"), any());
    }

    @Test
    void refreshToken_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RefreshToken oldToken = new RefreshToken(UUID.randomUUID(), "old-refresh", userId,
                java.time.Instant.now().plusSeconds(3600), false);

        when(loadRefreshTokenPort.loadByToken("old-refresh")).thenReturn(Optional.of(oldToken));
        when(loadUserPort.loadUserById(userId)).thenReturn(Optional.empty());
        when(securityPort.getCurrentIp()).thenReturn("127.0.0.1");

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken("old-refresh"));
    }
}
