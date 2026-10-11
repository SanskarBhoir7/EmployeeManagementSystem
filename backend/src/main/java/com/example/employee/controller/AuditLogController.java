package com.example.employee.controller;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    @Autowired
    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * View all audit logs (ADMIN only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponseDto>> getAllAuditLogs() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }

    /**
     * Filter audit logs by entity type (ADMIN only)
     */
    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponseDto>> getAuditLogsByEntity(@PathVariable String entityType) {
        return ResponseEntity.ok(auditService.getAuditLogsByEntity(entityType));
    }
}
