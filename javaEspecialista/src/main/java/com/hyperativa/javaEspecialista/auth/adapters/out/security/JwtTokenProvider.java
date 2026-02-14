package com.hyperativa.javaEspecialista.auth.adapters.out.security;

import com.hyperativa.javaEspecialista.auth.domain.model.User;
import com.hyperativa.javaEspecialista.auth.domain.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider implements AuthService.TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    @Value("${app.security.token-issuer:https://hyperativa.com}")
    private String tokenIssuer;

    public JwtTokenProvider(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public String generateToken(User user) {
        log.debug("Generating JWT token for user: {}", user.username());
        Instant now = Instant.now();
        String scope = user.roles().stream()
                .map(role -> role.name())
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(tokenIssuer)
                .issuedAt(now)
                .expiresAt(now.plus(expirationSeconds, ChronoUnit.SECONDS))
                .subject(user.username())
                .claim("scope", scope)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
