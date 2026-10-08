package com.example.employee.service;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.service.impl.EmployeeServiceImpl;
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

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeRequestDto requestDto;

    @BeforeEach
    void setUp() {
        employee = new Employee(1L, "Raj", "Malhotra", "raj.m@example.com", "9876543210",
                LocalDate.of(1995, 1, 1), "Male", "IT", "Developer",
                new BigDecimal("60000.00"), LocalDate.of(2022, 1, 1), "Mumbai", "Active");

        requestDto = new EmployeeRequestDto("Raj", "Malhotra", "raj.m@example.com", "9876543210",
                LocalDate.of(1995, 1, 1), "Male", "IT", "Developer",
                new BigDecimal("60000.00"), LocalDate.of(2022, 1, 1), "Mumbai", "Active");
    }

    @Test
    @DisplayName("Should create employee successfully when email is unique")
    void testCreateEmployeeSuccess() {
        when(employeeRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDto result = employeeService.createEmployee(requestDto);

        assertNotNull(result);
        assertEquals("Raj", result.getFirstName());
        assertEquals("raj.m@example.com", result.getEmail());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when creating with existing email")
    void testCreateEmployeeDuplicateEmail() {
        when(employeeRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> employeeService.createEmployee(requestDto));
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
    @DisplayName("Should update employee successfully")
    void testUpdateEmployeeSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot(requestDto.getEmail(), 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponseDto result = employeeService.updateEmployee(1L, requestDto);

        assertNotNull(result);
        assertEquals("Raj", result.getFirstName());
        verify(employeeRepository, times(1)).save(employee);
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
}
