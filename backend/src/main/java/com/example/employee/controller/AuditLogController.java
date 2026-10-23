package com.example.employee.controller;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit logs", description = "Administrative audit history")
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
    @Operation(summary = "List audit logs", description = "ADMIN only. Returns audit entries in reverse chronological order.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit log list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public ResponseEntity<List<AuditLogResponseDto>> getAllAuditLogs() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }

    /**
     * Filter audit logs by entity type (ADMIN only)
     */
    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter audit logs by entity type", description = "ADMIN only.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching audit entries"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public ResponseEntity<List<AuditLogResponseDto>> getAuditLogsByEntity(@Parameter(description = "Entity type, for example EMPLOYEE, DEPARTMENT, or LEAVE") @PathVariable String entityType) {
        return ResponseEntity.ok(auditService.getAuditLogsByEntity(entityType));
    }
}
