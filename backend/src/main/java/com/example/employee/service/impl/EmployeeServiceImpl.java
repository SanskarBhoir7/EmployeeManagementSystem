package com.example.employee.service.impl;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.dto.TerminateEmployeeRequestDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.entity.Role;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.service.EmployeeService;
import com.example.employee.validation.EmployeeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeValidator employeeValidator;
    private final UserRepository userRepository;

    @Autowired
    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeValidator employeeValidator,
                               UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.employeeValidator = employeeValidator;
        this.userRepository = userRepository;
    }

    @Override
    public EmployeeResponseDto createEmployee(EmployeeRequestDto dto) {
        // Step 1: Validate business constraints and dates
        employeeValidator.validate(dto);

        // Step 2: Check for unique email
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("An employee with email '" + dto.getEmail().trim() + "' already exists.");
        }

        Employee employee = new Employee();
        mapDtoToEntity(dto, employee);
        employee.setStatus(EmploymentStatus.ACTIVE);

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        return EmployeeResponseDto.fromEntity(employee);
    }

    @Override
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Step 1: Validate business constraints and dates
        employeeValidator.validate(dto);

        // Step 2: Ensure updated email doesn't conflict with another employee
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateEmailException("Another employee with email '" + dto.getEmail().trim() + "' already exists.");
        }

        // Step 3: Update editable employee fields while preserving status and termination fields
        mapDtoToEntity(dto, employee);
        // Ordinary employee-edit operations cannot change employment status or termination details

        Employee updated = employeeRepository.save(employee);
        return EmployeeResponseDto.fromEntity(updated);
    }

    @Override
    public EmployeeResponseDto terminateEmployee(Long id, TerminateEmployeeRequestDto dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Business Rule: Cannot terminate already terminated employee
        if (employee.getStatus() == EmploymentStatus.TERMINATED) {
            throw new IllegalStateException("Employee with ID " + id + " (" + employee.getFirstName() + " " +
                    employee.getLastName() + ") is already terminated.");
        }

        if (dto.getEffectiveDate() == null) {
            throw new IllegalArgumentException("Termination effective date is required.");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Termination reason is required.");
        }

        // Business Rule: Termination date cannot be before joining date
        if (dto.getEffectiveDate().isBefore(employee.getJoiningDate())) {
            throw new IllegalArgumentException("Termination effective date (" + dto.getEffectiveDate() +
                    ") cannot be before the employee's joining date (" + employee.getJoiningDate() + ").");
        }

        employee.setStatus(EmploymentStatus.TERMINATED);
        employee.setTerminationDate(dto.getEffectiveDate());
        employee.setTerminationReason(dto.getReason().trim());

        Employee saved = employeeRepository.save(employee);

        // Terminated account security: Disable any associated user account for this employee
        userRepository.findByEmployeeId(id).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });

        // Also check if any EMPLOYEE role account is linked via matching email
        userRepository.findByEmail(saved.getEmail()).ifPresent(user -> {
            if (user.getRole() == Role.EMPLOYEE) {
                user.setEnabled(false);
                userRepository.save(user);
            }
        });

        return EmployeeResponseDto.fromEntity(saved);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> searchEmployees(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.searchEmployees(query.trim()).stream()
                .map(EmployeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> filterEmployeesByDepartment(String department) {
        if (department == null || department.equalsIgnoreCase("ALL") || department.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.findByDepartmentIgnoreCase(department.trim()).stream()
                .map(EmployeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> filterEmployeesByStatus(String status) {
        if (status == null || status.equalsIgnoreCase("ALL") || status.trim().isEmpty()) {
            return getAllEmployees();
        }
        EmploymentStatus empStatus = parseEmploymentStatus(status);
        if (empStatus == null) {
            return Collections.emptyList();
        }
        return employeeRepository.findByStatus(empStatus).stream()
                .map(EmployeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> filterEmployees(String department, String status) {
        boolean hasDept = department != null && !department.equalsIgnoreCase("ALL") && !department.trim().isEmpty();
        boolean hasStatus = status != null && !status.equalsIgnoreCase("ALL") && !status.trim().isEmpty();

        List<Employee> result;
        if (hasDept && hasStatus) {
            EmploymentStatus empStatus = parseEmploymentStatus(status);
            if (empStatus != null) {
                result = employeeRepository.findByDepartmentIgnoreCaseAndStatus(department.trim(), empStatus);
            } else {
                result = Collections.emptyList();
            }
        } else if (hasDept) {
            result = employeeRepository.findByDepartmentIgnoreCase(department.trim());
        } else if (hasStatus) {
            EmploymentStatus empStatus = parseEmploymentStatus(status);
            if (empStatus != null) {
                result = employeeRepository.findByStatus(empStatus);
            } else {
                result = Collections.emptyList();
            }
        } else {
            result = employeeRepository.findAll();
        }

        return result.stream()
                .map(EmployeeResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private EmploymentStatus parseEmploymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EmploymentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void mapDtoToEntity(EmployeeRequestDto dto, Employee employee) {
        employee.setFirstName(dto.getFirstName().trim());
        employee.setLastName(dto.getLastName().trim());
        employee.setEmail(dto.getEmail().trim().toLowerCase());
        employee.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setGender(dto.getGender() != null ? dto.getGender().trim() : null);
        employee.setDepartment(dto.getDepartment().trim());
        employee.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);
        employee.setSalary(dto.getSalary());
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        // Note: Employment status and termination details are not changed by ordinary create/update mapping
    }
}
