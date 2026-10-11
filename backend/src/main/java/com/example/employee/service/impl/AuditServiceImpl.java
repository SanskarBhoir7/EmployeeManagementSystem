package com.example.employee.service.impl;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.entity.AuditAction;
import com.example.employee.entity.AuditLog;
import com.example.employee.repository.AuditLogRepository;
import com.example.employee.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String actor, AuditAction action, String entityType, Long entityId, String details) {
        if (actor == null || actor.trim().isEmpty()) {
            actor = "SYSTEM";
        }

        // Sanitize details to never expose credentials, passwords or tokens
        String sanitizedDetails = sanitizeDetails(details);

        AuditLog auditLog = new AuditLog(actor, action, entityType, entityId, sanitizedDetails);

        try {
            auditLogRepository.save(auditLog);
            log.info("Audit recorded: action={}, actor={}, entityType={}, entityId={}",
                    action, actor, entityType, entityId);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to persist audit log for action {} by {}: {}",
                    action, actor, ex.getMessage(), ex);
            // Requirement: Ensure audit failures are handled deliberately; do not silently claim an action was audited
            throw new RuntimeException("Audit logging failed. Operation cannot proceed without recording audit trail: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(AuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getAuditLogsByEntity(String entityType) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType)
                .stream()
                .map(AuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private String sanitizeDetails(String text) {
        if (text == null) return null;
        // Regex scrub any inadvertent password, secret or bearer tokens
        return text.replaceAll("(?i)(password|secret|token|bearer)\\s*[:=]\\s*[^,;\\s]+", "$1=***REDACTED***");
    }
}
