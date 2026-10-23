package com.example.employee.controller;

import com.example.employee.dto.ApiResponse;
import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.dto.EmployeePageResponseDto;
import com.example.employee.dto.EmployeeImportResponseDto;
import com.example.employee.dto.TerminateEmployeeRequestDto;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.service.EmployeeImportService;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee records, directory search, and employment lifecycle")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeImportService employeeImportService;
    private com.example.employee.repository.DepartmentRepository departmentRepository;
    private com.example.employee.repository.LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public EmployeeController(EmployeeService employeeService, EmployeeImportService employeeImportService) {
        this.employeeService = employeeService;
        this.employeeImportService = employeeImportService;
    }

    /**
     * Imports employees from CSV or XLSX. Each valid row is committed independently.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Import employees from CSV or XLSX", description = "ADMIN and HR only. Upload the file in the 'file' form field. Required columns: firstName, lastName, email, dateOfBirth, salary, joiningDate, and either department or departmentId. Optional columns: phone, gender, designation, address. Dates use yyyy-MM-dd. Department names match existing names case-insensitively after trimming; IDs must identify an existing department. New employees use the existing create workflow (ACTIVE status). Each valid row commits independently; rejected rows do not roll back prior successful rows.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Import summary, including rejected row numbers and reasons"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed file, missing/unsupported columns, file or row limit exceeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<EmployeeImportResponseDto> importEmployees(
            @Parameter(description = "CSV or XLSX file, maximum 5 MB and 1,000 rows after the header", required = true)
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(employeeImportService.importEmployees(file));
    }

    @Autowired(required = false)
    public void setDepartmentRepository(com.example.employee.repository.DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Autowired(required = false)
    public void setLeaveRequestRepository(com.example.employee.repository.LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Create a new employee (ADMIN and HR)
     * POST /api/employees
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create an employee", description = "ADMIN and HR only. Creates an employee record and returns the created employee.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Employee created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation or business-rule error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
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
    @Operation(summary = "List employees", description = "ADMIN and HR only. Returns a list-shaped response; optional department and status filters are applied by the server.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees(
            @Parameter(description = "Exact department name") @RequestParam(required = false) String department,
            @Parameter(description = "Employment status: Active, Inactive, or Terminated") @RequestParam(required = false) String status) {

        if ((department != null && !department.isBlank()) || (status != null && !status.isBlank())) {
            List<EmployeeResponseDto> filtered = employeeService.filterEmployees(department, status);
            return ResponseEntity.ok(filtered);
        }
        List<EmployeeResponseDto> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    /**
     * Server-paged employee directory. Page numbers are zero-based; size is limited to 100.
     * GET /api/employees/page?page=0&size=10&sort=name&direction=asc&query=rahul&department=IT&status=ACTIVE
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get a page of employees", description = "ADMIN and HR only. Searches name, email, and designation; combines search with department and status filters. Page is zero-based, page size is 1–100. Sort fields: id, name, department, designation, joiningDate, salary, status.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paged employee results and page metadata"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page, size, sort, direction, or status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<EmployeePageResponseDto> getEmployeeDirectoryPage(
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of employees per page (1–100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Approved sort field", example = "name") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Sort direction: asc or desc", example = "asc") @RequestParam(defaultValue = "asc") String direction,
            @Parameter(description = "Substring in employee name, email, or designation") @RequestParam(required = false) String query,
            @Parameter(description = "Exact department name") @RequestParam(required = false) String department,
            @Parameter(description = "Employment status: Active, Inactive, or Terminated") @RequestParam(required = false) String status) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }

        Sort pageSort = switch (sort) {
            case "id" -> Sort.by(sortDirection, "id");
            case "name" -> Sort.by(sortDirection, "firstName", "lastName");
            case "department" -> Sort.by(sortDirection, "legacyDepartment");
            case "designation" -> Sort.by(sortDirection, "designation");
            case "joiningDate" -> Sort.by(sortDirection, "joiningDate");
            case "salary" -> Sort.by(sortDirection, "salary");
            case "status" -> Sort.by(sortDirection, "status");
            default -> throw new IllegalArgumentException("Unsupported sort field.");
        };
        if (!"id".equals(sort)) {
            pageSort = pageSort.and(Sort.by(Sort.Direction.ASC, "id"));
        }

        EmploymentStatus employmentStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                employmentStatus = EmploymentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Status must be Active, Inactive, or Terminated.");
            }
        }

        Pageable pageable = PageRequest.of(page, size, pageSort);
        EmployeePageResponseDto result = employeeService.getEmployeeDirectoryPage(
                normalizeFilter(query), normalizeFilter(department), employmentStatus, pageable);
        return ResponseEntity.ok(result);
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Get employee by ID:
     * - ADMIN and HR can view any employee profile
     * - EMPLOYEE can ONLY view their own linked profile (#id == principal.employeeId)
     * GET /api/employees/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR') or (hasRole('EMPLOYEE') and #id == principal.employeeId)")
    @Operation(summary = "Get an employee", description = "ADMIN and HR can view any employee. EMPLOYEE can view only the linked profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee details"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Role or employee visibility rule denied access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@Parameter(description = "Employee ID", example = "1") @PathVariable Long id) {
        EmployeeResponseDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    /**
     * Update an existing employee (ADMIN and HR)
     * PUT /api/employees/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update an employee", description = "ADMIN and HR only. Updates editable employee fields.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation or business-rule error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<EmployeeResponseDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDto dto) {
        EmployeeResponseDto updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Terminate an active employee (ADMIN and HR only - EMPLOYEE forbidden)
     * POST /api/employees/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Terminate an employee", description = "ADMIN and HR only. Records the effective date and reason and disables any linked employee account.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee terminated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid termination request or employee already terminated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<EmployeeResponseDto> terminateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody TerminateEmployeeRequestDto dto) {
        EmployeeResponseDto terminated = employeeService.terminateEmployee(id, dto);
        return ResponseEntity.ok(terminated);
    }

    /**
     * Delete an employee (Strictly restricted to ADMIN - HR is blocked)
     * DELETE /api/employees/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an employee", description = "ADMIN only. Permanently deletes the employee record. Returns an ApiResponse envelope.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employee deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found")
    })
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@Parameter(description = "Employee ID", example = "1") @PathVariable Long id) {
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
    @Operation(summary = "Search employees", description = "ADMIN and HR only. Returns a list-shaped response. Searches employee name and email; use /api/employees/page for combined filters and pagination.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching employee list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<List<EmployeeResponseDto>> searchEmployees(
            @Parameter(description = "Substring in employee name or email") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Alias for name") @RequestParam(value = "query", required = false) String query) {

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
    @Operation(summary = "Filter employees by department", description = "ADMIN and HR only. Returns a list-shaped response.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching employee list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<List<EmployeeResponseDto>> filterByDepartment(@Parameter(description = "Exact department name") @PathVariable String department) {
        List<EmployeeResponseDto> results = employeeService.filterEmployeesByDepartment(department);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter employees by status (ADMIN and HR)
     * GET /api/employees/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Filter employees by status", description = "ADMIN and HR only. Returns a list-shaped response.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching employee list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<List<EmployeeResponseDto>> filterByStatus(@Parameter(description = "Employment status: Active, Inactive, or Terminated") @PathVariable String status) {
        List<EmployeeResponseDto> results = employeeService.filterEmployeesByStatus(status);
        return ResponseEntity.ok(results);
    }

    /**
     * Quick statistics for dashboard counters (ADMIN and HR)
     * GET /api/employees/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get dashboard statistics", description = "ADMIN and HR only. Returns counts for employees, departments, and leaves.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard counters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<EmployeeResponseDto> all = employeeService.getAllEmployees();
        long total = all.size();
        long active = all.stream().filter(e -> "Active".equalsIgnoreCase(e.getStatus())).count();
        long terminated = all.stream().filter(e -> "Terminated".equalsIgnoreCase(e.getStatus())).count();
        long inactive = all.stream().filter(e -> "Inactive".equalsIgnoreCase(e.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("terminated", terminated);
        stats.put("inactive", inactive);

        long departmentCount = (departmentRepository != null) ? departmentRepository.count() : 0L;
        stats.put("departments", departmentCount);

        long totalLeaves = 0L;
        long pendingLeaves = 0L;
        long approvedLeaves = 0L;
        long rejectedLeaves = 0L;
        if (leaveRequestRepository != null) {
            totalLeaves = leaveRequestRepository.count();
            pendingLeaves = leaveRequestRepository.countByStatus(com.example.employee.entity.LeaveStatus.PENDING);
            approvedLeaves = leaveRequestRepository.countByStatus(com.example.employee.entity.LeaveStatus.APPROVED);
            rejectedLeaves = leaveRequestRepository.countByStatus(com.example.employee.entity.LeaveStatus.REJECTED);
        }
        stats.put("totalLeaves", totalLeaves);
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("approvedLeaves", approvedLeaves);
        stats.put("rejectedLeaves", rejectedLeaves);

        return ResponseEntity.ok(stats);
    }
}
