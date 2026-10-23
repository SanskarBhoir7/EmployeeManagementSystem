package com.example.employee.service;

import com.example.employee.dto.EmployeeImportResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeeImportService {
    EmployeeImportResponseDto importEmployees(MultipartFile file);
}
