package com.example.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.employee.entity.Department;
import java.time.LocalDateTime;

@Schema(description = "Department details, including its employee count when provided by the service.")
public class DepartmentResponseDto {

    private Long id;
    private String name;
    private String description;
    private long employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DepartmentResponseDto() {
    }

    public DepartmentResponseDto(Long id, String name, String description, long employeeCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.employeeCount = employeeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DepartmentResponseDto fromEntity(Department department, long employeeCount) {
        if (department == null) {
            return null;
        }
        DepartmentResponseDto dto = new DepartmentResponseDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setEmployeeCount(employeeCount);
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(long employeeCount) {
        this.employeeCount = employeeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
