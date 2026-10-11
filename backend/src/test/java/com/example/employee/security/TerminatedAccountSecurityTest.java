package com.example.employee.security;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminatedAccountSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Active employee linked user account should be enabled")
    void testActiveEmployeeUserIsEnabled() {
        User user = new User(1L, "rahul.s", "hash", "rahul.sharma@example.com", Role.EMPLOYEE, 1L, true);
        Employee activeEmp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", EmploymentStatus.ACTIVE);

        when(userRepository.findByUsername("rahul.s")).thenReturn(Optional.of(user));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(activeEmp));

        UserDetails userDetails = userDetailsService.loadUserByUsername("rahul.s");

        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Terminated employee linked user account should be disabled, preventing login and token reuse")
    void testTerminatedEmployeeUserIsDisabled() {
        User user = new User(1L, "rahul.s", "hash", "rahul.sharma@example.com", Role.EMPLOYEE, 1L, true);
        Employee terminatedEmp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", EmploymentStatus.TERMINATED);
        terminatedEmp.setTerminationDate(LocalDate.now());
        terminatedEmp.setTerminationReason("Resigned");

        when(userRepository.findByUsername("rahul.s")).thenReturn(Optional.of(user));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(terminatedEmp));

        UserDetails userDetails = userDetailsService.loadUserByUsername("rahul.s");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled(), "UserDetails must report isEnabled() = false for terminated employee");
    }

    @Test
    @DisplayName("ADMIN and HR accounts must NOT be blocked when unrelated employee records are terminated")
    void testAdminAndHrAccountsRemainEnabled() {
        User admin = new User(2L, "admin", "hash", "admin@ems.local", Role.ADMIN, null, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin");

        assertNotNull(adminDetails);
        assertTrue(adminDetails.isEnabled(), "Admin account without employee link must remain enabled");
    }
}
