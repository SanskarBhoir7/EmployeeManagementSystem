package com.example.employee.service;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.entity.AuditAction;
import com.example.employee.entity.AuditLog;
import com.example.employee.repository.AuditLogRepository;
import com.example.employee.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = new AuditLog("admin", AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", 1L, "Created employee John Doe");
        sampleLog.setId(10L);
        sampleLog.setTimestamp(LocalDateTime.now());
    }

    @Test
    @DisplayName("log: Successfully persists audit log with sanitized details")
    void testLog_Success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log("admin", AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", 1L, "Created employee Alice");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("admin", saved.getActor());
        assertEquals(AuditAction.EMPLOYEE_CREATED, saved.getAction());
        assertEquals("EMPLOYEE", saved.getEntityType());
        assertEquals(1L, saved.getEntityId());
        assertEquals("Created employee Alice", saved.getDetails());
    }

    @Test
    @DisplayName("log: Redacts sensitive passwords or tokens from audit log details")
    void testLog_RedactsSensitiveData() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log("admin", AuditAction.EMPLOYEE_UPDATED, "EMPLOYEE", 1L, "Updated password: secret123, token=xyz789");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertFalse(saved.getDetails().contains("secret123"));
        assertFalse(saved.getDetails().contains("xyz789"));
        assertTrue(saved.getDetails().contains("***REDACTED***"));
    }

    @Test
    @DisplayName("log: Deliberately throws exception when database persistence fails")
    void testLog_PersistenceFailure_ThrowsRuntimeException() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB disk full"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auditService.log("admin", AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", 1L, "Created employee")
        );

        assertTrue(ex.getMessage().contains("Audit logging failed"));
    }

    @Test
    @DisplayName("getAllAuditLogs: Returns all logs mapped to DTOs in descending order")
    void testGetAllAuditLogs() {
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(sampleLog));

        List<AuditLogResponseDto> logs = auditService.getAllAuditLogs();

        assertEquals(1, logs.size());
        assertEquals(10L, logs.get(0).getId());
        assertEquals("admin", logs.get(0).getActor());
        assertEquals(AuditAction.EMPLOYEE_CREATED, logs.get(0).getAction());
    }

    @Test
    @DisplayName("getAuditLogsByEntity: Returns entity-filtered audit logs")
    void testGetAuditLogsByEntity() {
        when(auditLogRepository.findByEntityTypeOrderByTimestampDesc("EMPLOYEE")).thenReturn(List.of(sampleLog));

        List<AuditLogResponseDto> logs = auditService.getAuditLogsByEntity("EMPLOYEE");

        assertEquals(1, logs.size());
        assertEquals("EMPLOYEE", logs.get(0).getEntityType());
    }
}
