package com.example.employee.service;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.*;
import com.example.employee.exception.InvalidLeaveStateException;
import com.example.employee.exception.LeaveNotFoundException;
import com.example.employee.exception.LeaveOverlapException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.LeaveRequestRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private User employeeUser;
    private User hrUser;
    private User adminUser;
    private Employee activeEmployee;
    private Employee terminatedEmployee;
    private LeaveRequest sampleLeave;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Engineering");

        activeEmployee = new Employee();
        activeEmployee.setId(10L);
        activeEmployee.setFirstName("Alice");
        activeEmployee.setLastName("Smith");
        activeEmployee.setEmail("alice@ems.local");
        activeEmployee.setDepartment(dept);
        activeEmployee.setDesignation("Developer");
        activeEmployee.setSalary(new BigDecimal("75000.00"));
        activeEmployee.setStatus(EmploymentStatus.ACTIVE);
        activeEmployee.setJoiningDate(LocalDate.now().minusMonths(6));

        terminatedEmployee = new Employee();
        terminatedEmployee.setId(20L);
        terminatedEmployee.setFirstName("Bob");
        terminatedEmployee.setLastName("Brown");
        terminatedEmployee.setEmail("bob@ems.local");
        terminatedEmployee.setStatus(EmploymentStatus.TERMINATED);
        terminatedEmployee.setJoiningDate(LocalDate.now().minusYears(1));

        employeeUser = new User();
        employeeUser.setId(100L);
        employeeUser.setUsername("alice");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setEmployeeId(10L);

        hrUser = new User();
        hrUser.setId(200L);
        hrUser.setUsername("hruser");
        hrUser.setRole(Role.HR);
        hrUser.setEmployeeId(30L); // distinct employee ID

        adminUser = new User();
        adminUser.setId(300L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        sampleLeave = new LeaveRequest();
        sampleLeave.setId(1L);
        sampleLeave.setEmployee(activeEmployee);
        sampleLeave.setLeaveType(LeaveType.PAID);
        sampleLeave.setStartDate(LocalDate.now().plusDays(5));
        sampleLeave.setEndDate(LocalDate.now().plusDays(7));
        sampleLeave.setReason("Annual vacation");
        sampleLeave.setStatus(LeaveStatus.PENDING);
    }

    @Test
    @DisplayName("submitLeave: Successfully submits leave for active employee")
    void testSubmitLeave_Success() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(LeaveType.PAID);
        dto.setStartDate(LocalDate.now().plusDays(5));
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setReason("Annual vacation");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(activeEmployee));
        when(leaveRequestRepository.existsOverlappingLeave(eq(10L), any(), any(), any())).thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest req = invocation.getArgument(0);
            req.setId(101L);
            return req;
        });

        LeaveResponseDto result = leaveService.submitLeave(dto, "alice");

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(LeaveStatus.PENDING, result.getStatus());
        assertEquals("Alice Smith", result.getEmployeeName());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("submitLeave: Rejects leave submission if employee is terminated")
    void testSubmitLeave_TerminatedEmployee_ThrowsException() {
        User terminatedUser = new User();
        terminatedUser.setUsername("bob");
        terminatedUser.setRole(Role.EMPLOYEE);
        terminatedUser.setEmployeeId(20L);

        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(LeaveType.SICK);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(2));
        dto.setReason("Feeling unwell");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(terminatedUser));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(terminatedEmployee));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                leaveService.submitLeave(dto, "bob")
        );

        assertTrue(ex.getMessage().contains("Terminated employees cannot submit leave requests"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitLeave: Rejects when start date is in the past")
    void testSubmitLeave_StartDateInPast_ThrowsException() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.now().minusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(1));
        dto.setReason("Casual time off");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(activeEmployee));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                leaveService.submitLeave(dto, "alice")
        );

        assertTrue(ex.getMessage().contains("cannot be in the past"));
    }

    @Test
    @DisplayName("submitLeave: Rejects when end date is before start date")
    void testSubmitLeave_EndDateBeforeStartDate_ThrowsException() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.now().plusDays(5));
        dto.setEndDate(LocalDate.now().plusDays(3));
        dto.setReason("Casual time off");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(activeEmployee));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                leaveService.submitLeave(dto, "alice")
        );

        assertTrue(ex.getMessage().contains("cannot be before start date"));
    }

    @Test
    @DisplayName("submitLeave: Rejects when overlapping leave exists")
    void testSubmitLeave_OverlappingLeave_ThrowsOverlapException() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.now().plusDays(5));
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setReason("Overlap test");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(activeEmployee));
        when(leaveRequestRepository.existsOverlappingLeave(eq(10L), any(), any(), any())).thenReturn(true);

        assertThrows(LeaveOverlapException.class, () ->
                leaveService.submitLeave(dto, "alice")
        );
    }

    @Test
    @DisplayName("reviewLeave: HR can approve pending leave")
    void testReviewLeave_Approve_Success() {
        LeaveReviewDto reviewDto = new LeaveReviewDto();
        reviewDto.setStatus(LeaveStatus.APPROVED);
        reviewDto.setReviewComments("Approved by HR");

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));
        when(userRepository.findByUsername("hruser")).thenReturn(Optional.of(hrUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveService.reviewLeave(1L, reviewDto, "hruser");

        assertEquals(LeaveStatus.APPROVED, result.getStatus());
        assertEquals("hruser", result.getReviewedBy());
        assertEquals("Approved by HR", result.getReviewComments());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    @DisplayName("reviewLeave: Reviewers cannot review their own leave request")
    void testReviewLeave_SelfReview_ThrowsIllegalStateException() {
        // HR user who owns this leave request
        User selfReviewer = new User();
        selfReviewer.setUsername("alice-hr");
        selfReviewer.setRole(Role.HR);
        selfReviewer.setEmployeeId(10L); // Matches activeEmployee ID 10L

        LeaveReviewDto reviewDto = new LeaveReviewDto();
        reviewDto.setStatus(LeaveStatus.APPROVED);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));
        when(userRepository.findByUsername("alice-hr")).thenReturn(Optional.of(selfReviewer));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                leaveService.reviewLeave(1L, reviewDto, "alice-hr")
        );

        assertTrue(ex.getMessage().contains("Reviewers are not permitted to review their own leave requests"));
    }

    @Test
    @DisplayName("reviewLeave: Cannot review an already reviewed or non-pending request")
    void testReviewLeave_AlreadyApproved_ThrowsInvalidLeaveStateException() {
        sampleLeave.setStatus(LeaveStatus.APPROVED);

        LeaveReviewDto reviewDto = new LeaveReviewDto();
        reviewDto.setStatus(LeaveStatus.REJECTED);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));

        assertThrows(InvalidLeaveStateException.class, () ->
                leaveService.reviewLeave(1L, reviewDto, "hruser")
        );
    }

    @Test
    @DisplayName("cancelLeave: Employee can cancel their own pending leave")
    void testCancelLeave_OwnPendingLeave_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveService.cancelLeave(1L, "alice");

        assertEquals(LeaveStatus.CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("cancelLeave: Employee cannot cancel another employee's leave")
    void testCancelLeave_OtherEmployee_ThrowsAccessDeniedException() {
        User otherEmployee = new User();
        otherEmployee.setUsername("charlie");
        otherEmployee.setRole(Role.EMPLOYEE);
        otherEmployee.setEmployeeId(99L); // Not 10L

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(otherEmployee));

        assertThrows(AccessDeniedException.class, () ->
                leaveService.cancelLeave(1L, "charlie")
        );
    }

    @Test
    @DisplayName("cancelLeave: Cannot cancel already approved leave")
    void testCancelLeave_AlreadyApproved_ThrowsInvalidLeaveStateException() {
        sampleLeave.setStatus(LeaveStatus.APPROVED);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(sampleLeave));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(employeeUser));

        assertThrows(InvalidLeaveStateException.class, () ->
                leaveService.cancelLeave(1L, "alice")
        );
    }
}
