package com.example.employee.service;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.dto.TerminateEmployeeRequestDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.service.impl.EmployeeServiceImpl;
import com.example.employee.validation.EmployeeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeValidator employeeValidator;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeRequestDto requestDto;

    @BeforeEach
    void setUp() {
        employee = new Employee(1L, "Raj", "Malhotra", "raj.m@example.com", "9876543210",
                LocalDate.of(1995, 1, 1), "Male", "IT", "Developer",
                new BigDecimal("60000.00"), LocalDate.of(2022, 1, 1), "Mumbai", EmploymentStatus.ACTIVE);

        requestDto = new EmployeeRequestDto("Raj", "Malhotra", "raj.m@example.com", "9876543210",
                LocalDate.of(1995, 1, 1), "Male", "IT", "Developer",
                new BigDecimal("60000.00"), LocalDate.of(2022, 1, 1), "Mumbai", "Active");
    }

    @Test
    @DisplayName("Should create employee successfully when email is unique and validator passes")
    void testCreateEmployeeSuccess() {
        when(employeeRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDto result = employeeService.createEmployee(requestDto);

        assertNotNull(result);
        assertEquals("Raj", result.getFirstName());
        assertEquals("raj.m@example.com", result.getEmail());
        verify(employeeValidator, times(1)).validate(requestDto);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when creating with existing email")
    void testCreateEmployeeDuplicateEmail() {
        when(employeeRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> employeeService.createEmployee(requestDto));
        verify(employeeValidator, times(1)).validate(requestDto);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should return employee by ID when exists")
    void testGetEmployeeByIdSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponseDto result = employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("raj.m@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when ID does not exist")
    void testGetEmployeeByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    @DisplayName("Should update employee successfully and preserve employment status")
    void testUpdateEmployeeSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(requestDto.getEmail(), 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        requestDto.setStatus("Terminated"); // Attempting to alter status via ordinary edit

        EmployeeResponseDto result = employeeService.updateEmployee(1L, requestDto);

        assertNotNull(result);
        assertEquals("Raj", result.getFirstName());
        // Status should remain ACTIVE despite DTO value
        assertEquals("ACTIVE", result.getStatus());
        verify(employeeValidator, times(1)).validate(requestDto);
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Should terminate active employee and deactivate linked user account")
    void testTerminateEmployeeSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User linkedUser = new User(10L, "raj.m", "hash", "raj.m@example.com", Role.EMPLOYEE, 1L, true);
        when(userRepository.findByEmployeeId(1L)).thenReturn(Optional.of(linkedUser));
        when(userRepository.findByEmail("raj.m@example.com")).thenReturn(Optional.of(linkedUser));

        LocalDate termDate = LocalDate.of(2023, 6, 30);
        TerminateEmployeeRequestDto termDto = new TerminateEmployeeRequestDto(termDate, "Resigned for higher studies");

        EmployeeResponseDto response = employeeService.terminateEmployee(1L, termDto);

        assertNotNull(response);
        assertEquals("TERMINATED", response.getStatus());
        assertEquals(termDate, response.getTerminationDate());
        assertEquals("Resigned for higher studies", response.getTerminationReason());

        // Historical details retained
        assertEquals(employee.getJoiningDate(), response.getJoiningDate());
        assertEquals(employee.getDateOfBirth(), response.getDateOfBirth());
        assertEquals(employee.getSalary(), response.getSalary());

        // Linked user account deactivated
        assertFalse(linkedUser.isEnabled());
        verify(userRepository, atLeastOnce()).save(linkedUser);
    }

    @Test
    @DisplayName("Should reject repeated termination on an already terminated employee")
    void testRepeatedTerminationRejected() {
        employee.setStatus(EmploymentStatus.TERMINATED);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        TerminateEmployeeRequestDto termDto = new TerminateEmployeeRequestDto(LocalDate.now(), "Duplicate term");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> employeeService.terminateEmployee(1L, termDto));
        assertTrue(ex.getMessage().contains("already terminated"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject termination effective date before joining date")
    void testTerminationDateBeforeJoiningDateRejected() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Joining date is 2022-01-01, effective date 2021-12-31
        TerminateEmployeeRequestDto termDto = new TerminateEmployeeRequestDto(
                LocalDate.of(2021, 12, 31), "Invalid date");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeeService.terminateEmployee(1L, termDto));
        assertTrue(ex.getMessage().contains("cannot be before the employee's joining date"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when terminating nonexistent employee")
    void testTerminateNonexistentEmployee() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        TerminateEmployeeRequestDto termDto = new TerminateEmployeeRequestDto(LocalDate.now(), "Resignation");

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.terminateEmployee(999L, termDto));
    }

    @Test
    @DisplayName("Should delete employee successfully when exists")
    void testDeleteEmployeeSuccess() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(1L);

        assertDoesNotThrow(() -> employeeService.deleteEmployee(1L));
        verify(employeeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when deleting nonexistent employee")
    void testDeleteEmployeeNotFound() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(99L));
        verify(employeeRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should search employees by name or email")
    void testSearchEmployees() {
        when(employeeRepository.searchEmployees("Raj")).thenReturn(Arrays.asList(employee));

        List<EmployeeResponseDto> results = employeeService.searchEmployees("Raj");

        assertEquals(1, results.size());
        assertEquals("Raj", results.get(0).getFirstName());
    }

    @Test
    @DisplayName("Should filter employees by TERMINATED status")
    void testFilterEmployeesByTerminatedStatus() {
        employee.setStatus(EmploymentStatus.TERMINATED);
        when(employeeRepository.findByStatus(EmploymentStatus.TERMINATED)).thenReturn(Arrays.asList(employee));

        List<EmployeeResponseDto> results = employeeService.filterEmployeesByStatus("Terminated");

        assertEquals(1, results.size());
        assertEquals("TERMINATED", results.get(0).getStatus());
    }
}
