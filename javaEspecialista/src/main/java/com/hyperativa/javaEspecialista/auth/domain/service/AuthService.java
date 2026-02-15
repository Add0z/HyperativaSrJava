package com.hyperativa.javaEspecialista.auth.domain.service;

import com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException;
import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;

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
    private final AuditPort auditPort;
    private final SecurityPort securityPort;

    public AuthService(LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            TokenProvider tokenProvider,
            MetricsPort metricsService,
            AuditPort auditPort,
            SecurityPort securityPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.metricsService = metricsService;
        this.auditPort = auditPort;
        this.securityPort = securityPort;
    }

    @Override
    public String login(String username, String password) {
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
        return tokenProvider.generateToken(user);
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

    public interface TokenProvider {
        String generateToken(User user);
    }
}
