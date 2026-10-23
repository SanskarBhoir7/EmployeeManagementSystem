package com.example.employee.controller;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.LeaveStatus;
import com.example.employee.service.LeaveService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Leave requests", description = "Employee leave submission, review, and cancellation")
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
    @Operation(summary = "Submit a leave request", description = "Any authenticated user may submit. Employees submit for themselves; ADMIN and HR may specify an employee ID.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Leave request created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation or leave-rule error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not allowed to submit for the requested employee"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Overlapping leave request")
    })
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
    @Operation(summary = "List my leave requests", description = "Returns only the authenticated user's leave requests. Available at /my and /my-leaves.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user's leave requests"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
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
    @Operation(summary = "List leave requests", description = "ADMIN and HR only. Optionally filters by leave status.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leave request list"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required")
    })
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves(@Parameter(description = "Optional leave status") @RequestParam(required = false) LeaveStatus status) {
        List<LeaveResponseDto> leaves = leaveService.getAllLeaves(status);
        return ResponseEntity.ok(leaves);
    }

    /**
     * Get single leave request by ID
     * GET /api/leaves/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a leave request", description = "An authenticated employee can view their own request. ADMIN and HR may view requests according to the service access rules.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leave request details"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Leave request is not visible to this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveResponseDto> getLeaveById(@Parameter(description = "Leave request ID", example = "1") @PathVariable Long id,
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
    @Operation(summary = "Review a leave request", description = "ADMIN and HR only. Sets the request to APPROVED or REJECTED.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leave request reviewed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid review or state transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN or HR role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveResponseDto> reviewLeave(@Parameter(description = "Leave request ID", example = "1") @PathVariable Long id,
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
    @Operation(summary = "Cancel a leave request", description = "An employee may cancel their own pending request. ADMIN and HR access follows the service rules.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leave request cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request cannot be cancelled in its current state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Leave request is not visible to this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveResponseDto> cancelLeave(@Parameter(description = "Leave request ID", example = "1") @PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        LeaveResponseDto response = leaveService.cancelLeave(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
