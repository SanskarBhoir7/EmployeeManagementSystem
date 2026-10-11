package com.example.employee.controller;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.LeaveStatus;
import com.example.employee.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /**
     * Submit leave request
     * POST /api/leaves
     * All authenticated users can submit.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponseDto> submitLeave(@Valid @RequestBody LeaveRequestDto dto,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        LeaveResponseDto response = leaveService.submitLeave(dto, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get authenticated user's own leave requests
     * GET /api/leaves/my or /api/leaves/my-leaves
     */
    @GetMapping({"/my", "/my-leaves"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveResponseDto>> getMyLeaves(@AuthenticationPrincipal UserDetails userDetails) {
        List<LeaveResponseDto> myLeaves = leaveService.getMyLeaves(userDetails.getUsername());
        return ResponseEntity.ok(myLeaves);
    }

    /**
     * Get all leave requests (ADMIN and HR only)
     * GET /api/leaves?status=PENDING
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves(@RequestParam(required = false) LeaveStatus status) {
        List<LeaveResponseDto> leaves = leaveService.getAllLeaves(status);
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get single leave request by ID
     * GET /api/leaves/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponseDto> getLeaveById(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        LeaveResponseDto leave = leaveService.getLeaveById(id, userDetails.getUsername());
        return ResponseEntity.ok(leave);
    }

    /**
     * Review (Approve or Reject) a leave request (ADMIN and HR only)
     * PUT /api/leaves/{id}/review
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LeaveResponseDto> reviewLeave(@PathVariable Long id,
                                                        @Valid @RequestBody LeaveReviewDto reviewDto,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        LeaveResponseDto response = leaveService.reviewLeave(id, reviewDto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a pending leave request
     * PUT /api/leaves/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponseDto> cancelLeave(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        LeaveResponseDto response = leaveService.cancelLeave(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
