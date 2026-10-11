package com.example.employee.controller;

import com.example.employee.dto.ApiResponse;
import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.service.EmployeeService;
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
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Create a new employee (ADMIN and HR)
     * POST /api/employees
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeRequestDto dto) {
        EmployeeResponseDto created = employeeService.createEmployee(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Get all employees, with optional department and status query params (ADMIN and HR)
     * GET /api/employees?department=IT&status=Active
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {

        if ((department != null && !department.isBlank()) || (status != null && !status.isBlank())) {
            List<EmployeeResponseDto> filtered = employeeService.filterEmployees(department, status);
            return ResponseEntity.ok(filtered);
        }
        List<EmployeeResponseDto> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    /**
     * Get employee by ID:
     * - ADMIN and HR can view any employee profile
     * - EMPLOYEE can ONLY view their own linked profile (#id == principal.employeeId)
     * GET /api/employees/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') or (hasRole('EMPLOYEE') and #id == principal.employeeId)")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    /**
     * Update an existing employee (ADMIN and HR)
     * PUT /api/employees/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDto dto) {
        EmployeeResponseDto updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an employee (Strictly restricted to ADMIN - HR is blocked)
     * DELETE /api/employees/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee with ID " + id + " was successfully deleted."));
    }

    /**
     * Search employees by name or email (ADMIN and HR)
     * GET /api/employees/search?name=Rahul
     * GET /api/employees/search?query=Rahul
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<EmployeeResponseDto>> searchEmployees(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "query", required = false) String query) {

        String searchTerm = (query != null && !query.isBlank()) ? query : name;
        List<EmployeeResponseDto> results = employeeService.searchEmployees(searchTerm);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter employees by department (ADMIN and HR)
     * GET /api/employees/department/{department}
     */
    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<EmployeeResponseDto>> filterByDepartment(@PathVariable String department) {
        List<EmployeeResponseDto> results = employeeService.filterEmployeesByDepartment(department);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter employees by status (ADMIN and HR)
     * GET /api/employees/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<EmployeeResponseDto>> filterByStatus(@PathVariable String status) {
        List<EmployeeResponseDto> results = employeeService.filterEmployeesByStatus(status);
        return ResponseEntity.ok(results);
    }

    /**
     * Quick statistics for dashboard counters (ADMIN and HR)
     * GET /api/employees/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<EmployeeResponseDto> all = employeeService.getAllEmployees();
        long total = all.size();
        long active = all.stream().filter(e -> "Active".equalsIgnoreCase(e.getStatus())).count();
        long inactive = total - active;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", inactive);
        return ResponseEntity.ok(stats);
    }
}
