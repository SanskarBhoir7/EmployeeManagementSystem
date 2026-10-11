package com.example.employee.controller;

import com.example.employee.dto.DepartmentRequestDto;
import com.example.employee.dto.DepartmentResponseDto;
import com.example.employee.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Autowired
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * List all departments (ADMIN and HR)
     * GET /api/departments
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<DepartmentResponseDto>> getAllDepartments() {
        List<DepartmentResponseDto> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    /**
     * View department by ID (ADMIN and HR)
     * GET /api/departments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable Long id) {
        DepartmentResponseDto department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    /**
     * Create new department (ADMIN only)
     * POST /api/departments
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> createDepartment(@Valid @RequestBody DepartmentRequestDto dto) {
        DepartmentResponseDto created = departmentService.createDepartment(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update department by ID (ADMIN only)
     * PUT /api/departments/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(@PathVariable Long id,
                                                                 @Valid @RequestBody DepartmentRequestDto dto) {
        DepartmentResponseDto updated = departmentService.updateDepartment(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete department by ID (ADMIN only, only if employeeCount == 0)
     * DELETE /api/departments/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Department with ID " + id + " deleted successfully.");
        return ResponseEntity.ok(response);
    }
}
