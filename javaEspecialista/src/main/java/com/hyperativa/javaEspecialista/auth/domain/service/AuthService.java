package com.hyperativa.javaEspecialista.auth.domain.service;

import com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException;
import com.hyperativa.javaEspecialista.auth.domain.model.RefreshToken;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.TokenPair;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadRefreshTokenPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveRefreshTokenPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service for authentication operations.
 * Framework-free: no Spring annotations or imports.
 * Wired via {@link com.hyperativa.javaEspecialista.config.AuthDomainConfig}.
 */
public class AuthService implements AuthInputPort {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProvider tokenProvider;
    private final MetricsPort metricsService;
    private final AuditPort auditPort;
    private final SecurityPort securityPort;
    private final long refreshTokenDurationSeconds;

    public AuthService(LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            SaveRefreshTokenPort saveRefreshTokenPort,
            LoadRefreshTokenPort loadRefreshTokenPort,
            PasswordEncoderPort passwordEncoder,
            TokenProvider tokenProvider,
            MetricsPort metricsService,
            AuditPort auditPort,
            SecurityPort securityPort,
            long refreshTokenDurationSeconds) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.loadRefreshTokenPort = loadRefreshTokenPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.metricsService = metricsService;
        this.auditPort = auditPort;
        this.securityPort = securityPort;
        this.refreshTokenDurationSeconds = refreshTokenDurationSeconds;
    }

    @Override
    public TokenPair login(String username, String password) {
        log.debug("Login attempt for username: {}", username);
        String ip = securityPort.getCurrentIp();
        auditPort.log(username, "LOGIN_ATTEMPT", null, ip, "PENDING", null);

        User user = loadUserPort.loadUserByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    metricsService.incrementLoginFailure("user_not_found");
                    auditPort.log(username, "LOGIN_FAILURE", null, ip, "FAILURE", "User not found");
                    return new AuthenticationException("Invalid username or password");
                });

        log.debug("User found, verifying credentials for: {}", username);

        boolean matches = passwordEncoder.matches(password, user.password());
        log.debug("Password matches: {}", matches);

        if (!matches) {
            log.warn("Password mismatch for user: {}", username);
            metricsService.incrementLoginFailure("bad_credentials");
            auditPort.log(username, "LOGIN_FAILURE", null, ip, "FAILURE", "Bad credentials");
            throw new AuthenticationException("Invalid username or password");
        }

        log.info("User authenticated successfully: {}", username);
        metricsService.incrementLoginSuccess();
        auditPort.log(username, "LOGIN_SUCCESS", null, ip, "SUCCESS", null);

        return createTokenPair(user);
    }

    @Override
    public TokenPair refreshToken(String refreshTokenStr) {
        String ip = securityPort.getCurrentIp();
        log.debug("Refresh token attempt");

        RefreshToken oldToken = loadRefreshTokenPort.loadByToken(refreshTokenStr)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (oldToken.revoked()) {
            log.warn("Attempt to use revoked refresh token: {}. Possible theft! Revoking all tokens for user: {}",
                    refreshTokenStr, oldToken.userId());
            saveRefreshTokenPort.revokeAllUserTokens(oldToken.userId());
            auditPort.log(oldToken.userId().toString(), "REFRESH_TOKEN_THEFT_DETECTED", null, ip, "FAILURE",
                    "Revoked token usage attempt");
            throw new AuthenticationException("Invalid refresh token");
        }

        if (oldToken.isExpired()) {
            log.warn("Attempt to use expired refresh token: {}", refreshTokenStr);
            throw new AuthenticationException("Expired refresh token");
        }

        // Token Rotation: Revoke old, Create new
        RefreshToken revokedToken = new RefreshToken(
                oldToken.id(),
                oldToken.token(),
                oldToken.userId(),
                oldToken.expiryDate(),
                true // Revoked
        );
        saveRefreshTokenPort.save(revokedToken);

        // Load user to get current roles/details for new access token
        User user = loadUserPort.loadUserById(oldToken.userId())
                .orElseThrow(() -> new AuthenticationException("User not found for refresh token"));

        return createTokenPair(user);
    }

    // Helper
    private TokenPair createTokenPair(User user) {
        AccessToken accessToken = tokenProvider.generateAccessToken(user);

        RefreshToken refreshToken = new RefreshToken(
                null, // ID generated by adapter
                UUID.randomUUID().toString(),
                user.id(),
                Instant.now().plus(refreshTokenDurationSeconds, ChronoUnit.SECONDS),
                false);

        RefreshToken savedRefresh = saveRefreshTokenPort.save(refreshToken);

        return new TokenPair(
                accessToken.token(),
                savedRefresh.token(),
                "Bearer",
                accessToken.expiresIn());
    }

    public interface TokenProvider {
        AccessToken generateAccessToken(User user);
    }

    public record AccessToken(String token, long expiresIn) {
    }

    @Override
    public void register(String username, String password, Set<Role> roles) {
        String ip = securityPort.getCurrentIp();
        auditPort.log(username, "REGISTER_ATTEMPT", null, ip, "PENDING", null);

        if (loadUserPort.loadUserByUsername(username).isPresent()) {
            metricsService.incrementUserRegistrationFailure("username_exists");
            auditPort.log(username, "REGISTER_FAILURE", null, ip, "FAILURE", "Username already exists");
            throw new com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException(username);
        }

        String encodedPassword = passwordEncoder.encode(password);
        Set<Role> assignedRoles = (roles == null || roles.isEmpty()) ? Set.of(Role.USER) : roles;
        User newUser = new User(java.util.UUID.randomUUID(), username, encodedPassword, assignedRoles);

        saveUserPort.save(newUser);
        metricsService.incrementUserRegistered(assignedRoles.toString());
        auditPort.log(username, "REGISTER_SUCCESS", newUser.id().toString(), ip, "SUCCESS", "Roles: " + assignedRoles);
    }
}
