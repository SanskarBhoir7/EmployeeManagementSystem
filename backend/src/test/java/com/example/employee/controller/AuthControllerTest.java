package com.example.employee.controller;

import com.example.employee.dto.ChangePasswordRequestDto;
import com.example.employee.dto.LoginRequestDto;
import com.example.employee.dto.LoginResponseDto;
import com.example.employee.dto.RegisterUserRequestDto;
import com.example.employee.dto.UserProfileDto;
import com.example.employee.entity.Role;
import com.example.employee.security.CustomAccessDeniedHandler;
import com.example.employee.security.CustomUserDetailsService;
import com.example.employee.security.JwtAuthenticationEntryPoint;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.SecurityConfig;
import com.example.employee.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 with JWT token for valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "Admin@123");
        LoginResponseDto responseDto = new LoginResponseDto("mock.jwt.token", "admin", "admin@ems.local", Role.ADMIN, null);

        when(authService.login(any(LoginRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 when credentials are bad")
    void testLoginBadCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("admin", "WrongPass");

        when(authService.login(any(LoginRequestDto.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/auth/me - Should return 200 with current user profile")
    void testGetCurrentUser() throws Exception {
        UserProfileDto profileDto = new UserProfileDto(1L, "admin", "admin@ems.local", Role.ADMIN, null, null);
        when(authService.getCurrentUserProfile("admin")).thenReturn(profileDto);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/auth/register - Should return 201 for ADMIN")
    void testRegisterUserAllowedForAdmin() throws Exception {
        RegisterUserRequestDto request = new RegisterUserRequestDto("newhr", "hr@ems.local", "HrPass123", Role.HR, null);
        UserProfileDto responseDto = new UserProfileDto(2L, "newhr", "hr@ems.local", Role.HR, null, null);

        when(authService.registerUser(any(RegisterUserRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newhr"));
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("POST /api/auth/register - Should return 403 Forbidden for HR role")
    void testRegisterUserForbiddenForHR() throws Exception {
        RegisterUserRequestDto request = new RegisterUserRequestDto("newemp", "emp@ems.local", "EmpPass123", Role.EMPLOYEE, null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("PUT /api/auth/change-password - Should return 200 on password change")
    void testChangePasswordSuccess() throws Exception {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("OldPass", "NewPass123");
        doNothing().when(authService).changePassword(eq("admin"), any(ChangePasswordRequestDto.class));

        mockMvc.perform(put("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/me - Should return 401 Unauthorized for anonymous user")
    void testGetCurrentUserAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 401 Unauthorized for unauthenticated request")
    void testRegisterUserUnauthenticated() throws Exception {
        RegisterUserRequestDto request = new RegisterUserRequestDto("newuser", "user@ems.local", "UserPass123", Role.HR, null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PUT /api/auth/change-password - Should return 401 Unauthorized for unauthenticated request")
    void testChangePasswordUnauthenticated() throws Exception {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("OldPass", "NewPass123");

        mockMvc.perform(put("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

