package com.example.employee.controller;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.LeaveStatus;
import com.example.employee.entity.LeaveType;
import com.example.employee.exception.LeaveNotFoundException;
import com.example.employee.exception.LeaveOverlapException;
import com.example.employee.security.CustomAccessDeniedHandler;
import com.example.employee.security.CustomUserDetailsService;
import com.example.employee.security.JwtAuthenticationEntryPoint;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.SecurityConfig;
import com.example.employee.service.LeaveService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveService leaveService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private LeaveResponseDto sampleLeaveResponse;
    private LeaveRequestDto sampleLeaveRequest;

    @BeforeEach
    void setUp() {
        sampleLeaveResponse = new LeaveResponseDto();
        sampleLeaveResponse.setId(1L);
        sampleLeaveResponse.setEmployeeId(10L);
        sampleLeaveResponse.setEmployeeName("Rahul Sharma");
        sampleLeaveResponse.setLeaveType(LeaveType.CASUAL);
        sampleLeaveResponse.setStartDate(LocalDate.now().plusDays(2));
        sampleLeaveResponse.setEndDate(LocalDate.now().plusDays(4));
        sampleLeaveResponse.setDaysCount(3);
        sampleLeaveResponse.setReason("Family function");
        sampleLeaveResponse.setStatus(LeaveStatus.PENDING);
        sampleLeaveResponse.setCreatedAt(LocalDateTime.now());

        sampleLeaveRequest = new LeaveRequestDto(10L, LeaveType.CASUAL,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), "Family function");
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("POST /api/leaves - Employee should submit leave request successfully (201)")
    void testSubmitLeaveSuccess() throws Exception {
        when(leaveService.submitLeave(any(LeaveRequestDto.class), eq("rahul.s"))).thenReturn(sampleLeaveResponse);

        mockMvc.perform(post("/api/leaves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleLeaveRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("POST /api/leaves - Overlapping leave request should return 409 Conflict")
    void testSubmitLeaveOverlap() throws Exception {
        when(leaveService.submitLeave(any(LeaveRequestDto.class), eq("rahul.s")))
                .thenThrow(new LeaveOverlapException("Overlapping leave request"));

        mockMvc.perform(post("/api/leaves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleLeaveRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("GET /api/leaves/my - Employee should view their own leave requests (200)")
    void testGetMyLeaves() throws Exception {
        when(leaveService.getMyLeaves("rahul.s")).thenReturn(List.of(sampleLeaveResponse));

        mockMvc.perform(get("/api/leaves/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].leaveType").value("CASUAL"));
    }

    @Test
    @WithMockUser(username = "hrmanager", roles = "HR")
    @DisplayName("GET /api/leaves - HR should view all leave requests (200)")
    void testGetAllLeavesHr() throws Exception {
        when(leaveService.getAllLeaves(null)).thenReturn(List.of(sampleLeaveResponse));

        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("GET /api/leaves - EMPLOYEE should be forbidden from accessing organizational leaves (403)")
    void testGetAllLeavesEmployeeForbidden() throws Exception {
        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrmanager", roles = "HR")
    @DisplayName("PUT /api/leaves/{id}/review - HR should approve leave request (200)")
    void testReviewLeaveSuccess() throws Exception {
        sampleLeaveResponse.setStatus(LeaveStatus.APPROVED);
        sampleLeaveResponse.setReviewedBy("hrmanager");

        LeaveReviewDto reviewDto = new LeaveReviewDto(LeaveStatus.APPROVED, "Approved by HR");
        when(leaveService.reviewLeave(eq(1L), any(LeaveReviewDto.class), eq("hrmanager")))
                .thenReturn(sampleLeaveResponse);

        mockMvc.perform(put("/api/leaves/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("PUT /api/leaves/{id}/review - EMPLOYEE should be forbidden from reviewing leave (403)")
    void testReviewLeaveEmployeeForbidden() throws Exception {
        LeaveReviewDto reviewDto = new LeaveReviewDto(LeaveStatus.APPROVED, "Approved");

        mockMvc.perform(put("/api/leaves/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rahul.s", roles = "EMPLOYEE")
    @DisplayName("PUT /api/leaves/{id}/cancel - Employee should cancel their pending request (200)")
    void testCancelLeaveSuccess() throws Exception {
        sampleLeaveResponse.setStatus(LeaveStatus.CANCELLED);
        when(leaveService.cancelLeave(eq(1L), eq("rahul.s"))).thenReturn(sampleLeaveResponse);

        mockMvc.perform(put("/api/leaves/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
