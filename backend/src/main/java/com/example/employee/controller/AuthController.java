package com.example.employee.controller;

import com.example.employee.dto.ApiResponse;
import com.example.employee.dto.ChangePasswordRequestDto;
import com.example.employee.dto.LoginRequestDto;
import com.example.employee.dto.LoginResponseDto;
import com.example.employee.dto.RegisterUserRequestDto;
import com.example.employee.dto.UserProfileDto;
import com.example.employee.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User authentication endpoint (public)
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        LoginResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user profile
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDto profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * Register a new user account (restricted to ADMIN)
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> registerUser(@Valid @RequestBody RegisterUserRequestDto request) {
        UserProfileDto created = authService.registerUser(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Change password for the currently authenticated user
     * PUT /api/auth/change-password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully."));
    }
}
