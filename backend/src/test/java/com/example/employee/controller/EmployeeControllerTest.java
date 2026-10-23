package com.example.employee.controller;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.dto.EmployeePageResponseDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.Role;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.security.*;
import com.example.employee.service.EmployeeService;
import com.example.employee.service.EmployeeImportService;
import com.example.employee.dto.EmployeeImportResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private EmployeeImportService employeeImportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmployeeResponseDto sampleResponseDto;
    private EmployeeRequestDto sampleRequestDto;

    @BeforeEach
    void setUp() {
        Employee emp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", "Active");
        sampleResponseDto = EmployeeResponseDto.fromEntity(emp);

        sampleRequestDto = new EmployeeRequestDto("Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", "Active");
    }

    // ==========================================
    // 9 Original Verified Tests (with ADMIN role)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees - Should create employee and return 201 for ADMIN")
    void testCreateEmployee() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("rahul.sharma@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees - Should return 400 when validation fails")
    void testCreateEmployeeValidationFailure() throws Exception {
        sampleRequestDto.setFirstName(""); // invalid

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees - Should return 409 when duplicate email exists")
    void testCreateEmployeeDuplicateEmail() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequestDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees - Should return 200 with list for ADMIN")
    void testGetAllEmployees() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList(sampleResponseDto));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Rahul"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /api/employees/page - applies server pagination, sorting and combined filters")
    void testPaginatedDirectoryWithCombinedFiltersAndSort() throws Exception {
        EmployeePageResponseDto page = new EmployeePageResponseDto(
                Arrays.asList(sampleResponseDto), 21, 5, 1, 5);
        var expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "salary")
                .and(Sort.by(Sort.Direction.ASC, "id")));
        when(employeeService.getEmployeeDirectoryPage(eq("rahul"), eq("IT"),
                eq(com.example.employee.entity.EmploymentStatus.ACTIVE),
                eq(expectedPageable)))
                .thenReturn(page);

        mockMvc.perform(get("/api/employees/page")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "salary")
                        .param("direction", "desc")
                        .param("query", " rahul ")
                        .param("department", " IT ")
                        .param("status", "Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Rahul"))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/page - returns metadata for empty results")
    void testPaginatedDirectoryEmptyResults() throws Exception {
        when(employeeService.getEmployeeDirectoryPage(eq(null), eq(null), eq(null), any()))
                .thenReturn(new EmployeePageResponseDto(java.util.Collections.emptyList(), 0, 0, 0, 10));

        mockMvc.perform(get("/api/employees/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/page - rejects invalid page, page size and sort fields")
    void testPaginatedDirectoryRejectsInvalidParameters() throws Exception {
        mockMvc.perform(get("/api/employees/page").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/page").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/page").param("sort", "email"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /api/employees/page - is forbidden for employee role")
    void testPaginatedDirectoryForbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/employees/page"))
                .andExpect(status().isForbidden());
        verify(employeeService, never()).getEmployeeDirectoryPage(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/{id} - Should return 200 when exists for ADMIN")
    void testGetEmployeeByIdSuccess() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponseDto);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/{id} - Should return 404 when not found")
    void testGetEmployeeByIdNotFound() throws Exception {
        when(employeeService.getEmployeeById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/employees/{id} - Should return 200 on update for ADMIN")
    void testUpdateEmployee() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/employees/{id} - Should return 200 on delete for ADMIN")
    void testDeleteEmployee() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/search?name=Rahul - Should return search results for ADMIN")
    void testSearchEmployees() throws Exception {
        when(employeeService.searchEmployees("Rahul")).thenReturn(Arrays.asList(sampleResponseDto));

        mockMvc.perform(get("/api/employees/search").param("name", "Rahul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Rahul"));
    }

    // =======================================================
    // New Security & RBAC Access Control Tests
    // =======================================================

    @Test
    @DisplayName("Anonymous Request: GET /api/employees - Should reject with 401 or 403 when unauthenticated")
    void testGetAllEmployeesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @DisplayName("Paginated employee directory remains authenticated")
    void testPaginatedDirectoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/employees/page"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Top-level Actuator health path does not require authentication")
    void testActuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status != 401 && status != 403,
                            "The top-level health path should not require authentication");
                });
    }

    @Test
    @DisplayName("Nested health and other Actuator paths still require authentication")
    void testNestedActuatorPathsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/db"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("RBAC: DELETE /api/employees/{id} - Should return 403 Forbidden for HR role")
    void testDeleteEmployeeForbiddenForHR() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isForbidden());
        verify(employeeService, never()).deleteEmployee(anyLong());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("RBAC: GET /api/employees - Should return 403 Forbidden for EMPLOYEE role")
    void testGetAllEmployeesForbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("RBAC: DELETE /api/employees/1 - Should return 403 Forbidden for EMPLOYEE role")
    void testDeleteEmployeeForbiddenForEmployee() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isForbidden());
        verify(employeeService, never()).deleteEmployee(anyLong());
    }

    @Test
    @DisplayName("RBAC: GET /api/employees/1 - Should permit EMPLOYEE when viewing their own linked profile (ID 1)")
    void testGetOwnEmployeeProfileAllowedForEmployee() throws Exception {
        UserPrincipal principal = new UserPrincipal(3L, "rahul.s", "pass", "rahul.s@ems.local", Role.EMPLOYEE, 1L, true);
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponseDto);

        mockMvc.perform(get("/api/employees/1").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("RBAC: GET /api/employees/2 - Should return 403 Forbidden for EMPLOYEE trying to view another profile")
    void testGetOtherEmployeeProfileForbiddenForEmployee() throws Exception {
        UserPrincipal principal = new UserPrincipal(3L, "rahul.s", "pass", "rahul.s@ems.local", Role.EMPLOYEE, 1L, true);

        mockMvc.perform(get("/api/employees/2").with(user(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("RBAC: POST /api/employees - Should return 403 Forbidden for EMPLOYEE role")
    void testCreateEmployeeForbiddenForEmployee() throws Exception {
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("RBAC: PUT /api/employees/1 - Should return 403 Forbidden for EMPLOYEE role")
    void testUpdateEmployeeForbiddenForEmployee() throws Exception {
        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auth: DELETE /api/employees/1 - Should return 401 Unauthorized for unauthenticated request")
    void testDeleteEmployeeUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ==========================================
    // Phase 1.1: Termination & Validation Tests
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees/{id}/terminate - Should permit ADMIN and return 200")
    void testTerminateEmployeeAllowedForAdmin() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.now(), "Resignation approved");

        Employee terminatedEmp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", com.example.employee.entity.EmploymentStatus.TERMINATED);
        terminatedEmp.setTerminationDate(termDto.getEffectiveDate());
        terminatedEmp.setTerminationReason(termDto.getReason());

        when(employeeService.terminateEmployee(eq(1L), any(com.example.employee.dto.TerminateEmployeeRequestDto.class)))
                .thenReturn(EmployeeResponseDto.fromEntity(terminatedEmp));

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.terminationReason").value("Resignation approved"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/employees/{id}/terminate - Should permit HR and return 200")
    void testTerminateEmployeeAllowedForHR() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.now(), "Contract completed");

        Employee terminatedEmp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", com.example.employee.entity.EmploymentStatus.TERMINATED);
        terminatedEmp.setTerminationDate(termDto.getEffectiveDate());
        terminatedEmp.setTerminationReason(termDto.getReason());

        when(employeeService.terminateEmployee(eq(1L), any(com.example.employee.dto.TerminateEmployeeRequestDto.class)))
                .thenReturn(EmployeeResponseDto.fromEntity(terminatedEmp));

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/employees/{id}/terminate - Should return 403 Forbidden for EMPLOYEE role")
    void testTerminateEmployeeForbiddenForEmployee() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.now(), "Resignation");

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isForbidden());

        verify(employeeService, never()).terminateEmployee(anyLong(), any());
    }

    @Test
    @DisplayName("POST /api/employees/{id}/terminate - Should return 401 Unauthorized for unauthenticated request")
    void testTerminateEmployeeUnauthenticated() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.now(), "Resignation");

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees/{id}/terminate - Should return 400 when already terminated")
    void testTerminateEmployeeAlreadyTerminated() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.now(), "Duplicate term");

        when(employeeService.terminateEmployee(eq(1L), any(com.example.employee.dto.TerminateEmployeeRequestDto.class)))
                .thenThrow(new IllegalStateException("Employee with ID 1 is already terminated."));

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee with ID 1 is already terminated."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees/{id}/terminate - Should return 400 when termination date before DOJ")
    void testTerminateEmployeeDateBeforeJoiningDate() throws Exception {
        com.example.employee.dto.TerminateEmployeeRequestDto termDto =
                new com.example.employee.dto.TerminateEmployeeRequestDto(LocalDate.of(2020, 1, 1), "Invalid date");

        when(employeeService.terminateEmployee(eq(1L), any(com.example.employee.dto.TerminateEmployeeRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Termination effective date cannot be before the employee's joining date."));

        mockMvc.perform(post("/api/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(termDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Termination effective date cannot be before the employee's joining date."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees - Should reject future DOB with 400 Bad Request")
    void testCreateEmployeeFutureDobRejected() throws Exception {
        sampleRequestDto.setDateOfBirth(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/employees - Should reject negative salary with 400 Bad Request")
    void testCreateEmployeeNegativeSalaryRejected() throws Exception {
        sampleRequestDto.setSalary(new BigDecimal("-1000.00"));

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/employees/{id} - Should reject invalid email during update with 400 Bad Request")
    void testUpdateEmployeeInvalidEmailRejected() throws Exception {
        sampleRequestDto.setEmail("not-an-email");

        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/employees/stats - Should return dashboard statistics successfully")
    void testGetDashboardStats() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList(sampleResponseDto));

        mockMvc.perform(get("/api/employees/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.active").value(1))
                .andExpect(jsonPath("$.terminated").value(0))
                .andExpect(jsonPath("$.departments").isNumber())
                .andExpect(jsonPath("$.totalLeaves").isNumber())
                .andExpect(jsonPath("$.pendingLeaves").isNumber())
                .andExpect(jsonPath("$.approvedLeaves").isNumber())
                .andExpect(jsonPath("$.rejectedLeaves").isNumber());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void employeeImportIsAvailableToAdmin() throws Exception {
        when(employeeImportService.importEmployees(any())).thenReturn(new EmployeeImportResponseDto(1, 1, 0, java.util.List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv", "data".getBytes());

        mockMvc.perform(multipart("/api/employees/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1));
    }

    @Test
    @WithMockUser(roles = "HR")
    void employeeImportIsAvailableToHr() throws Exception {
        when(employeeImportService.importEmployees(any())).thenReturn(new EmployeeImportResponseDto(0, 0, 0, java.util.List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv", "header".getBytes());

        mockMvc.perform(multipart("/api/employees/import").file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeImportIsForbiddenToEmployee() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv", "header".getBytes());
        mockMvc.perform(multipart("/api/employees/import").file(file))
                .andExpect(status().isForbidden());
        verifyNoInteractions(employeeImportService);
    }

    @Test
    void employeeImportRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "employees.csv", "text/csv", "header".getBytes());
        mockMvc.perform(multipart("/api/employees/import").file(file))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(employeeImportService);
    }
}


