package com.hyperativa.javaEspecialista.audit.adapters.out.persistence;

import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.entity.AuditLogEntity;
import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.repository.AuditLogRepository;
import com.hyperativa.javaEspecialista.audit.domain.port.out.AuditPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing PCI DSS Requirement 10 audit logging.
 * Persists audit events to the audit_log table for compliance and forensic
 * analysis.
 */
@Component
public class AuditRepositoryAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditRepositoryAdapter.class);

    private final AuditLogRepository auditLogRepository;

    public AuditRepositoryAdapter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String userId, String action, String resourceId,
            String ipAddress, String result, String details) {
        try {
            var entity = new AuditLogEntity(userId, action, resourceId, ipAddress, result, details);
            auditLogRepository.save(entity);
            log.debug("Audit event recorded: action={}, user={}, result={}", action, userId, result);
        } catch (Exception e) {
            // Audit failures must never break business operations,
            // but we log them at ERROR level for monitoring
            log.error("Failed to persist audit event: action={}, user={}, error={}",
                    action, userId, e.getMessage(), e);
        }
    }
}
