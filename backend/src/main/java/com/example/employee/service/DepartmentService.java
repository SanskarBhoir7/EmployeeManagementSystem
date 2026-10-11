package com.example.employee.service;

import com.example.employee.dto.DepartmentRequestDto;
import com.example.employee.dto.DepartmentResponseDto;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponseDto> getAllDepartments();

    DepartmentResponseDto getDepartmentById(Long id);

    DepartmentResponseDto createDepartment(DepartmentRequestDto dto);

    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto);

    void deleteDepartment(Long id);
}
