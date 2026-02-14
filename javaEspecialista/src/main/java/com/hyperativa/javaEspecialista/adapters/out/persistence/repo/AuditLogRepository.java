package com.hyperativa.javaEspecialista.adapters.out.persistence.repo;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.AuditLogEntity;
import org.springframework.data.repository.CrudRepository;

public interface AuditLogRepository extends CrudRepository<AuditLogEntity, Long> {
}
