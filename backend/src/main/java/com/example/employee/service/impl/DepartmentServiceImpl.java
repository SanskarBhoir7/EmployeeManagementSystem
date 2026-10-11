package com.example.employee.service.impl;

import com.example.employee.dto.DepartmentRequestDto;
import com.example.employee.dto.DepartmentResponseDto;
import com.example.employee.entity.Department;
import com.example.employee.exception.DepartmentNotEmptyException;
import com.example.employee.exception.DepartmentNotFoundException;
import com.example.employee.exception.DuplicateDepartmentException;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private com.example.employee.service.AuditService auditService;

    @Autowired
    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                  EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Autowired(required = false)
    public void setAuditService(com.example.employee.service.AuditService auditService) {
        this.auditService = auditService;
    }

    private void recordAudit(com.example.employee.entity.AuditAction action, Long id, String details) {
        if (auditService != null) {
            String actor = "SYSTEM";
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                actor = auth.getName();
            }
            auditService.log(actor, action, "DEPARTMENT", id, details);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(dept -> {
                    long count = employeeRepository.countByDepartmentId(dept.getId());
                    return DepartmentResponseDto.fromEntity(dept, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        long count = employeeRepository.countByDepartmentId(dept.getId());
        return DepartmentResponseDto.fromEntity(dept, count);
    }

    @Override
    public DepartmentResponseDto createDepartment(DepartmentRequestDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be blank");
        }
        String trimmedName = dto.getName().trim();

        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateDepartmentException("Department with name '" + trimmedName + "' already exists");
        }

        Department department = new Department();
        department.setName(trimmedName);
        department.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Department saved = departmentRepository.save(department);
        recordAudit(com.example.employee.entity.AuditAction.DEPARTMENT_CREATED, saved.getId(),
                "Created department '" + saved.getName() + "'");
        return DepartmentResponseDto.fromEntity(saved, 0L);
    }

    @Override
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be blank");
        }
        String trimmedName = dto.getName().trim();

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateDepartmentException("Department with name '" + trimmedName + "' already exists");
        }

        dept.setName(trimmedName);
        dept.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);

        Department updated = departmentRepository.save(dept);
        long count = employeeRepository.countByDepartmentId(updated.getId());
        recordAudit(com.example.employee.entity.AuditAction.DEPARTMENT_UPDATED, updated.getId(),
                "Updated department '" + updated.getName() + "'");
        return DepartmentResponseDto.fromEntity(updated, count);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new DepartmentNotEmptyException("Cannot delete department '" + dept.getName() + "' because " +
                    employeeCount + " employee(s) are assigned to it.");
        }

        departmentRepository.delete(dept);
        recordAudit(com.example.employee.entity.AuditAction.DEPARTMENT_DELETED, id,
                "Deleted department '" + dept.getName() + "'");
    }
}
