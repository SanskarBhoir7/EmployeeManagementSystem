package com.example.employee.service.impl;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.entity.LeaveRequest;
import com.example.employee.entity.LeaveStatus;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.exception.InvalidLeaveStateException;
import com.example.employee.exception.LeaveNotFoundException;
import com.example.employee.exception.LeaveOverlapException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.LeaveRequestRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private com.example.employee.service.AuditService auditService;

    @Autowired
    public LeaveServiceImpl(LeaveRequestRepository leaveRequestRepository,
                            EmployeeRepository employeeRepository,
                            UserRepository userRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setAuditService(com.example.employee.service.AuditService auditService) {
        this.auditService = auditService;
    }

    private void recordAudit(String actor, com.example.employee.entity.AuditAction action, Long id, String details) {
        if (auditService != null) {
            auditService.log(actor, action, "LEAVE", id, details);
        }
    }

    @Override
    public LeaveResponseDto submitLeave(LeaveRequestDto dto, String authenticatedUsername) {
        User authUser = getUserByUsername(authenticatedUsername);

        Long targetEmployeeId = null;
        if (authUser.getRole() == Role.EMPLOYEE) {
            targetEmployeeId = authUser.getEmployeeId();
            if (targetEmployeeId == null) {
                throw new IllegalStateException("Your user account is not linked to an employee profile.");
            }
        } else {
            // ADMIN or HR can specify employeeId, or use their own if linked
            if (dto.getEmployeeId() != null) {
                targetEmployeeId = dto.getEmployeeId();
            } else if (authUser.getEmployeeId() != null) {
                targetEmployeeId = authUser.getEmployeeId();
            } else {
                throw new IllegalArgumentException("Employee ID is required for administrative leave submission.");
            }
        }

        final Long finalTargetEmployeeId = targetEmployeeId;
        Employee employee = employeeRepository.findById(finalTargetEmployeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(finalTargetEmployeeId));

        // Rule: Terminated employees must not submit leave requests
        if (employee.getStatus() == EmploymentStatus.TERMINATED) {
            throw new IllegalStateException("Terminated employees cannot submit leave requests.");
        }

        // Rule: Validate date boundaries
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Both start date and end date are required.");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Leave end date (" + dto.getEndDate() +
                    ") cannot be before start date (" + dto.getStartDate() + ").");
        }

        // Rule: Past date policy (cannot be in the past)
        LocalDate today = LocalDate.now();
        if (dto.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Leave start date (" + dto.getStartDate() +
                    ") cannot be in the past.");
        }

        // Rule: Start date cannot be before employee's joining date
        if (employee.getJoiningDate() != null && dto.getStartDate().isBefore(employee.getJoiningDate())) {
            throw new IllegalArgumentException("Leave start date (" + dto.getStartDate() +
                    ") cannot be before the employee's joining date (" + employee.getJoiningDate() + ").");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Leave reason is required.");
        }

        // Rule: Overlapping pending or approved requests check
        boolean hasOverlap = leaveRequestRepository.existsOverlappingLeave(
                employee.getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                Arrays.asList(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );

        if (hasOverlap) {
            throw new LeaveOverlapException("Employee already has an overlapping active or pending leave request between " +
                    dto.getStartDate() + " and " + dto.getEndDate() + ".");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(dto.getLeaveType());
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setReason(dto.getReason().trim());
        leaveRequest.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        recordAudit(authenticatedUsername, com.example.employee.entity.AuditAction.LEAVE_SUBMITTED, saved.getId(),
                "Submitted " + saved.getLeaveType() + " leave request for " + employee.getFirstName() + " " + employee.getLastName() +
                " from " + saved.getStartDate() + " to " + saved.getEndDate());
        return LeaveResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getMyLeaves(String authenticatedUsername) {
        User authUser = getUserByUsername(authenticatedUsername);
        if (authUser.getEmployeeId() == null) {
            return List.of();
        }
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(authUser.getEmployeeId())
                .stream()
                .map(LeaveResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getAllLeaves(LeaveStatus statusFilter) {
        List<LeaveRequest> requests;
        if (statusFilter != null) {
            requests = leaveRequestRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
        } else {
            requests = leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream()
                .map(LeaveResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponseDto getLeaveById(Long id, String authenticatedUsername) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveNotFoundException(id));

        User authUser = getUserByUsername(authenticatedUsername);
        if (authUser.getRole() == Role.EMPLOYEE) {
            if (authUser.getEmployeeId() == null || !authUser.getEmployeeId().equals(leave.getEmployee().getId())) {
                throw new AccessDeniedException("You are not authorized to view another employee's leave request.");
            }
        }

        return LeaveResponseDto.fromEntity(leave);
    }

    @Override
    public LeaveResponseDto reviewLeave(Long id, LeaveReviewDto reviewDto, String reviewerUsername) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveNotFoundException(id));

        // Rule: Only PENDING requests can be reviewed
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveStateException("Only PENDING leave requests can be reviewed. Current status is " +
                    leave.getStatus() + ".");
        }

        // Rule: Review status must be either APPROVED or REJECTED
        if (reviewDto.getStatus() != LeaveStatus.APPROVED && reviewDto.getStatus() != LeaveStatus.REJECTED) {
            throw new IllegalArgumentException("Review status must be either APPROVED or REJECTED.");
        }

        // Rule: Employees must not approve/review their own requests
        User reviewer = getUserByUsername(reviewerUsername);
        if (reviewer.getEmployeeId() != null && reviewer.getEmployeeId().equals(leave.getEmployee().getId())) {
            throw new IllegalStateException("Reviewers are not permitted to review their own leave requests.");
        }

        leave.setStatus(reviewDto.getStatus());
        leave.setReviewedBy(reviewerUsername);
        leave.setReviewedAt(LocalDateTime.now());
        leave.setReviewComments(reviewDto.getReviewComments() != null ? reviewDto.getReviewComments().trim() : null);

        LeaveRequest updated = leaveRequestRepository.save(leave);
        com.example.employee.entity.AuditAction auditAction = (reviewDto.getStatus() == LeaveStatus.APPROVED)
                ? com.example.employee.entity.AuditAction.LEAVE_APPROVED
                : com.example.employee.entity.AuditAction.LEAVE_REJECTED;
        recordAudit(reviewerUsername, auditAction, updated.getId(),
                "Leave request #" + updated.getId() + " " + reviewDto.getStatus() + " by " + reviewerUsername +
                (reviewDto.getReviewComments() != null ? ". Comments: " + reviewDto.getReviewComments() : ""));
        return LeaveResponseDto.fromEntity(updated);
    }

    @Override
    public LeaveResponseDto cancelLeave(Long id, String authenticatedUsername) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new LeaveNotFoundException(id));

        User authUser = getUserByUsername(authenticatedUsername);
        // Authorization: Employee must own the request unless ADMIN
        if (authUser.getRole() == Role.EMPLOYEE) {
            if (authUser.getEmployeeId() == null || !authUser.getEmployeeId().equals(leave.getEmployee().getId())) {
                throw new AccessDeniedException("You can only cancel your own leave requests.");
            }
        }

        // Rule: Only PENDING requests can be cancelled
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveStateException("Only PENDING leave requests can be cancelled. Current status is " +
                    leave.getStatus() + ".");
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest updated = leaveRequestRepository.save(leave);
        recordAudit(authenticatedUsername, com.example.employee.entity.AuditAction.LEAVE_CANCELLED, updated.getId(),
                "Cancelled leave request #" + updated.getId() + " by " + authenticatedUsername);
        return LeaveResponseDto.fromEntity(updated);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));
    }
}
