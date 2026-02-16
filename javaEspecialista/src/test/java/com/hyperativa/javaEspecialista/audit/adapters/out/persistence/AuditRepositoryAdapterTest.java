package com.hyperativa.javaEspecialista.audit.adapters.out.persistence;

import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.entity.AuditLogEntity;
import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRepositoryAdapterTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditRepositoryAdapter adapter;

    @Test
    void log_WhenDetailsAreJson_ShouldSaveAsIs() {
        String jsonDetails = "{\"key\":\"value\"}";
        adapter.log("user1", "ACTION", "res1", "127.0.0.1", "SUCCESS", jsonDetails);

        verify(auditLogRepository).save(argThat(entity -> entity.getDetails().equals(jsonDetails) &&
                entity.getUserId().equals("user1")));
    }

    @Test
    void log_WhenDetailsArePlainString_ShouldWrapInQuotes() {
        String plainDetails = "Just a string";
        adapter.log("user1", "ACTION", "res1", "127.0.0.1", "SUCCESS", plainDetails);

        verify(auditLogRepository).save(argThat(entity -> entity.getDetails().equals("\"Just a string\"")));
    }

    @Test
    void log_WhenRepositoryThrowsException_ShouldCatchAndLog() {
        doThrow(new RuntimeException("DB Error")).when(auditLogRepository).save(any(AuditLogEntity.class));

        // Should not throw exception
        adapter.log("user1", "ACTION", "res1", "127.0.0.1", "SUCCESS", "details");

        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }
}
