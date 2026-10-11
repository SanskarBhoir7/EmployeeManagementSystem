package com.example.employee.controller;

import com.example.employee.dto.AuditLogResponseDto;
import com.example.employee.entity.AuditAction;
import com.example.employee.security.CustomAccessDeniedHandler;
import com.example.employee.security.CustomUserDetailsService;
import com.example.employee.security.JwtAuthenticationEntryPoint;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.SecurityConfig;
import com.example.employee.service.AuditService;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private AuditLogResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = new AuditLogResponseDto();
        sampleDto.setId(1L);
        sampleDto.setActor("admin");
        sampleDto.setAction(AuditAction.EMPLOYEE_CREATED);
        sampleDto.setEntityType("EMPLOYEE");
        sampleDto.setEntityId(10L);
        sampleDto.setDetails("Created employee Alice");
        sampleDto.setTimestamp(LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/audit-logs: ADMIN can view all audit logs (200 OK)")
    void testGetAllAuditLogs_AsAdmin_Returns200() throws Exception {
        when(auditService.getAllAuditLogs()).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actor").value("admin"))
                .andExpect(jsonPath("$[0].action").value("EMPLOYEE_CREATED"));
    }

    @Test
    @WithMockUser(username = "hruser", roles = {"HR"})
    @DisplayName("GET /api/audit-logs: HR is forbidden from viewing audit logs (403 Forbidden)")
    void testGetAllAuditLogs_AsHR_Returns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "empuser", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/audit-logs: EMPLOYEE is forbidden from viewing audit logs (403 Forbidden)")
    void testGetAllAuditLogs_AsEmployee_Returns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/audit-logs: Unauthenticated request is rejected (401 Unauthorized)")
    void testGetAllAuditLogs_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/audit-logs/entity/{entityType}: ADMIN can filter logs by entity type")
    void testGetAuditLogsByEntity_AsAdmin_Returns200() throws Exception {
        when(auditService.getAuditLogsByEntity("EMPLOYEE")).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/audit-logs/entity/EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entityType").value("EMPLOYEE"));
    }
}
