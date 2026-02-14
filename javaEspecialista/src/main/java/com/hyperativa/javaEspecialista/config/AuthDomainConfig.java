package com.hyperativa.javaEspecialista.config;

import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadUserPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.PasswordEncoderPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveUserPort;
import com.hyperativa.javaEspecialista.auth.domain.service.AuthService;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the auth domain service without Spring annotations in the domain.
 * Mirrors the pattern used by {@link DomainConfig} for CardService.
 */
@Configuration
public class AuthDomainConfig {

    @Bean
    public AuthInputPort authService(LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            PasswordEncoderPort passwordEncoderPort,
            AuthService.TokenProvider tokenProvider,
            MetricsPort metricsPort) {
        return new AuthService(loadUserPort, saveUserPort, passwordEncoderPort, tokenProvider, metricsPort);
    }
}
