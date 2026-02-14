package com.hyperativa.javaEspecialista.domain.ports.out;

/**
 * Port for PCI DSS Requirement 10 compliant audit logging.
 * Records all access to cardholder data for security compliance.
 */
public interface AuditPort {

    void log(String userId, String action, String resourceId, String ipAddress, String result, String details);
}
