package com.hyperativa.javaEspecialista.auth.domain.service;

import com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Domain service for authentication operations.
 * Framework-free: no Spring annotations or imports.
 * Wired via {@link com.hyperativa.javaEspecialista.config.AuthDomainConfig}.
 */
public class AuthService implements AuthInputPort {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProvider tokenProvider;
    private final MetricsPort metricsService;

    public AuthService(LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            TokenProvider tokenProvider,
            MetricsPort metricsService) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.metricsService = metricsService;
    }

    @Override
    public String login(String username, String password) {
        log.debug("Login attempt for username: {}", username);

        User user = loadUserPort.loadUserByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    metricsService.incrementLoginFailure("user_not_found");
                    return new AuthenticationException("Invalid username or password");
                });

        log.debug("User found, verifying credentials for: {}", username);

        boolean matches = passwordEncoder.matches(password, user.password());
        log.debug("Password matches: {}", matches);

        if (!matches) {
            log.warn("Password mismatch for user: {}", username);
            metricsService.incrementLoginFailure("bad_credentials");
            throw new AuthenticationException("Invalid username or password");
        }

        log.info("User authenticated successfully: {}", username);
        metricsService.incrementLoginSuccess();
        return tokenProvider.generateToken(user);
    }

    @Override
    public void register(String username, String password) {
        if (loadUserPort.loadUserByUsername(username).isPresent()) {
            metricsService.incrementUserRegistrationFailure("username_exists");
            throw new com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException(username);
        }

        String encodedPassword = passwordEncoder.encode(password);
        Set<Role> roles = Set.of(Role.USER);
        User newUser = new User(java.util.UUID.randomUUID(), username, encodedPassword, roles);

        saveUserPort.save(newUser);
        metricsService.incrementUserRegistered("USER");
    }

    public interface TokenProvider {
        String generateToken(User user);
    }
}
