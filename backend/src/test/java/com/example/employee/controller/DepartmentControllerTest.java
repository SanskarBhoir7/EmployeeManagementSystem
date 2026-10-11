package com.example.employee.controller;

import com.example.employee.dto.DepartmentRequestDto;
import com.example.employee.dto.DepartmentResponseDto;
import com.example.employee.exception.DepartmentNotEmptyException;
import com.example.employee.exception.DepartmentNotFoundException;
import com.example.employee.exception.DuplicateDepartmentException;
import com.example.employee.security.CustomAccessDeniedHandler;
import com.example.employee.security.CustomUserDetailsService;
import com.example.employee.security.JwtAuthenticationEntryPoint;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.SecurityConfig;
import com.example.employee.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private DepartmentResponseDto sampleDeptResponse;
    private DepartmentRequestDto sampleDeptRequest;

    @BeforeEach
    void setUp() {
        sampleDeptResponse = new DepartmentResponseDto(1L, "Engineering", "Software and DevOps", 5L,
                LocalDateTime.now(), LocalDateTime.now());

        sampleDeptRequest = new DepartmentRequestDto("Engineering", "Software and DevOps");
    }

    // ==========================================
    // GET /api/departments (List)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/departments - ADMIN should view all departments (200)")
    void testGetAllDepartmentsAdmin() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(sampleDeptResponse));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Engineering"))
                .andExpect(jsonPath("$[0].employeeCount").value(5));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /api/departments - HR should view all departments (200)")
    void testGetAllDepartmentsHr() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(sampleDeptResponse));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /api/departments - EMPLOYEE should be forbidden (403)")
    void testGetAllDepartmentsEmployeeForbidden() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/departments - Unauthenticated user should be unauthorized (401)")
    void testGetAllDepartmentsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // GET /api/departments/{id} (View By ID)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/departments/{id} - ADMIN should view department by ID (200)")
    void testGetDepartmentByIdAdmin() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(sampleDeptResponse);

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /api/departments/{id} - HR should view department by ID (200)")
    void testGetDepartmentByIdHr() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(sampleDeptResponse);

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /api/departments/{id} - EMPLOYEE should be forbidden (403)")
    void testGetDepartmentByIdEmployeeForbidden() throws Exception {
        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/departments/{id} - Nonexistent ID should return 404")
    void testGetDepartmentByIdNotFound() throws Exception {
        when(departmentService.getDepartmentById(999L)).thenThrow(new DepartmentNotFoundException(999L));

        mockMvc.perform(get("/api/departments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department with ID 999 not found"));
    }

    // ==========================================
    // POST /api/departments (Create)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/departments - ADMIN should create department (201)")
    void testCreateDepartmentAdminSuccess() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequestDto.class))).thenReturn(sampleDeptResponse);

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/departments - HR should be forbidden (403)")
    void testCreateDepartmentHrForbidden() throws Exception {
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/departments - EMPLOYEE should be forbidden (403)")
    void testCreateDepartmentEmployeeForbidden() throws Exception {
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/departments - Blank name should return 400 Bad Request")
    void testCreateDepartmentBlankName() throws Exception {
        DepartmentRequestDto badDto = new DepartmentRequestDto("", "Desc");

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/departments - Duplicate name should return 409 Conflict")
    void testCreateDepartmentDuplicateName() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequestDto.class)))
                .thenThrow(new DuplicateDepartmentException("Department with name 'Engineering' already exists"));

        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Department with name 'Engineering' already exists"));
    }

    // ==========================================
    // PUT /api/departments/{id} (Update)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/departments/{id} - ADMIN should update department (200)")
    void testUpdateDepartmentAdminSuccess() throws Exception {
        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequestDto.class)))
                .thenReturn(sampleDeptResponse);

        mockMvc.perform(put("/api/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("PUT /api/departments/{id} - HR should be forbidden (403)")
    void testUpdateDepartmentHrForbidden() throws Exception {
        mockMvc.perform(put("/api/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("PUT /api/departments/{id} - EMPLOYEE should be forbidden (403)")
    void testUpdateDepartmentEmployeeForbidden() throws Exception {
        mockMvc.perform(put("/api/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/departments/{id} - Duplicate name on update should return 409 Conflict")
    void testUpdateDepartmentDuplicateName() throws Exception {
        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequestDto.class)))
                .thenThrow(new DuplicateDepartmentException("Department with name 'Engineering' already exists"));

        mockMvc.perform(put("/api/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/departments/{id} - Nonexistent ID should return 404")
    void testUpdateDepartmentNotFound() throws Exception {
        when(departmentService.updateDepartment(eq(999L), any(DepartmentRequestDto.class)))
                .thenThrow(new DepartmentNotFoundException(999L));

        mockMvc.perform(put("/api/departments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDeptRequest)))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // DELETE /api/departments/{id} (Delete)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/departments/{id} - ADMIN deletes empty department successfully (200)")
    void testDeleteDepartmentAdminSuccess() throws Exception {
        doNothing().when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department with ID 1 deleted successfully."));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("DELETE /api/departments/{id} - HR should be forbidden (403)")
    void testDeleteDepartmentHrForbidden() throws Exception {
        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("DELETE /api/departments/{id} - EMPLOYEE should be forbidden (403)")
    void testDeleteDepartmentEmployeeForbidden() throws Exception {
        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/departments/{id} - Department with assigned employees returns 409 Conflict")
    void testDeleteDepartmentWithEmployeesConflict() throws Exception {
        doThrow(new DepartmentNotEmptyException("Cannot delete department 'Engineering' because 5 employee(s) are assigned to it."))
                .when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete department 'Engineering' because 5 employee(s) are assigned to it."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/departments/{id} - Nonexistent ID returns 404")
    void testDeleteDepartmentNotFound() throws Exception {
        doThrow(new DepartmentNotFoundException(999L)).when(departmentService).deleteDepartment(999L);

        mockMvc.perform(delete("/api/departments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
