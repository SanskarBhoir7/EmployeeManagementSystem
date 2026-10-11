package com.example.employee.service;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.entity.AuditAction;

import java.util.List;

public interface AuditService {

    void log(String actor, AuditAction action, String entityType, Long entityId, String details);

    List<AuditLogResponseDto> getAllAuditLogs();

    List<AuditLogResponseDto> getAuditLogsByEntity(String entityType);
}
