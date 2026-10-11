package com.example.employee.repository;

import com.example.employee.entity.AuditAction;
import com.example.employee.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);

    List<AuditLog> findByActorOrderByTimestampDesc(String actor);

    List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);
}
