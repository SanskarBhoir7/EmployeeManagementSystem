package com.example.employee.dto;

import com.example.employee.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful login result. Copy token into Swagger UI's Authorize dialog; do not include the Bearer prefix.")
public class LoginResponseDto {

    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private Role role;
    private Long employeeId;

    public LoginResponseDto() {
    }

    public LoginResponseDto(String token, String username, String email, Role role, Long employeeId) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
        this.employeeId = employeeId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
