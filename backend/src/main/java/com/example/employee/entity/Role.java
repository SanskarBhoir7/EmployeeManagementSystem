package com.example.employee.entity;

public enum Role {
    ADMIN,
    HR,
    EMPLOYEE;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
