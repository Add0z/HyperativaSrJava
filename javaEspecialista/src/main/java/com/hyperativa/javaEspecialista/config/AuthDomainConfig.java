package com.hyperativa.javaEspecialista.config;

import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.auth.domain.service.AuthService;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the auth domain service without Spring annotations in the domain.
 * Mirrors the pattern used by {@link DomainConfig} for CardService.
 */
@Configuration
public class AuthDomainConfig {

    @Bean
    public AuthService authService(LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoder,
            AuthService.TokenProvider tokenProvider,
            MetricsPort metricsPort,
            AuditPort auditPort,
            SecurityPort securityPort) {
        return new AuthService(loadUserPort, saveUserPort, passwordEncoder, tokenProvider, metricsPort, auditPort,
                securityPort);
    }
}
