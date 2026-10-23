package com.example.employee.service;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.dto.EmployeePageResponseDto;
import com.example.employee.dto.TerminateEmployeeRequestDto;
import com.example.employee.entity.EmploymentStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {

    EmployeeResponseDto createEmployee(EmployeeRequestDto dto);

    List<EmployeeResponseDto> getAllEmployees();

    EmployeePageResponseDto getEmployeeDirectoryPage(String query, String department,
                                                      EmploymentStatus status, Pageable pageable);

    EmployeeResponseDto getEmployeeById(Long id);

    EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto dto);

    EmployeeResponseDto terminateEmployee(Long id, TerminateEmployeeRequestDto dto);

    void deleteEmployee(Long id);

    List<EmployeeResponseDto> searchEmployees(String query);

    List<EmployeeResponseDto> filterEmployeesByDepartment(String department);

    List<EmployeeResponseDto> filterEmployeesByStatus(String status);

    List<EmployeeResponseDto> filterEmployees(String department, String status);
}
