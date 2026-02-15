package com.hyperativa.javaEspecialista.config;

import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import com.hyperativa.javaEspecialista.domain.ports.out.CryptoPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import com.hyperativa.javaEspecialista.domain.ports.out.SecurityPort;
import com.hyperativa.javaEspecialista.domain.service.CardService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public CardService cardService(CardRepositoryPort cardRepositoryPort, CryptoPort cryptoPort,
            MetricsPort metricsPort, AuditPort auditPort, SecurityPort securityPort) {
        return new CardService(cardRepositoryPort, cryptoPort, metricsPort, auditPort, securityPort);
    }
}
