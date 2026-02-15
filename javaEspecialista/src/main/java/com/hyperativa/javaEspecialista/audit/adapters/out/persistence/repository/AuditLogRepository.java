package com.hyperativa.javaEspecialista.audit.adapters.out.persistence.repository;

import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.entity.AuditLogEntity;
import org.springframework.data.repository.CrudRepository;

public interface AuditLogRepository extends CrudRepository<AuditLogEntity, Long> {
}
