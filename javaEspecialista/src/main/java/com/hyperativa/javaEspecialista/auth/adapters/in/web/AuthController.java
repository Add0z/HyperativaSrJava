package com.hyperativa.javaEspecialista.auth.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user management APIs")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthInputPort authService;

    public AuthController(AuthInputPort authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns a JWT Bearer token.")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        log.info("Login attempt for user: {}", request.username());
        String token = authService.login(request.username(), request.password());
        log.info("Login successful for user: {}", request.username());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user account with default USER role.")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.username());
        Set<Role> roles = new HashSet<>();
        if (request.roles() != null && !request.roles().isEmpty()) {
            for (String roleName : request.roles()) {
                try {
                    roles.add(Role.valueOf(roleName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role provided: {}", roleName);
                    // Optionally throw exception or ignore invalid roles.
                    // For now, let's ignore or maybe better to throw validation error?
                    // The prompt asks for "not admin user" handling, implying we want to create
                    // admin.
                }
            }
        }

        authService.register(request.username(), request.password(), roles);
        log.info("User registered successfully: {}", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record AuthRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 12, message = "Password must be at least 12 characters") @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter") @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter") @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit") @Pattern(regexp = ".*[@$!%*?&].*", message = "Password must contain at least one special character (@$!%*?&)") String password,
            Set<String> roles) {
    }

    public record AuthResponse(String token) {
    }
}
