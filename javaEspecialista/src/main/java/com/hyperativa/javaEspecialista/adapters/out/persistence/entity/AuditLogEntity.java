package com.hyperativa.javaEspecialista.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("audit_log")
public class AuditLogEntity {

    @Id
    private Long id;
    private LocalDateTime timestamp;
    private String userId;
    private String action;
    private String resourceId;
    private String ipAddress;
    private String result;
    private String details;

    public AuditLogEntity() {
    }

    public AuditLogEntity(String userId, String action, String resourceId,
            String ipAddress, String result, String details) {
        this.timestamp = LocalDateTime.now();
        this.userId = userId;
        this.action = action;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.result = result;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }
}
