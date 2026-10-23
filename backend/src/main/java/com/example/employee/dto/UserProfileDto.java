package com.example.employee.dto;

import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated or newly created user account profile, with optional linked employee identity.")
public class UserProfileDto {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private Long employeeId;
    private String employeeName;

    public UserProfileDto() {
    }

    public UserProfileDto(Long id, String username, String email, Role role, Long employeeId, String employeeName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public static UserProfileDto fromEntity(User user, String employeeName) {
        if (user == null) {
            return null;
        }
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getEmployeeId(),
                employeeName
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
