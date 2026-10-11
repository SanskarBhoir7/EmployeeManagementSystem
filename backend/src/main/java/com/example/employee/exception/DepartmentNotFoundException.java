package com.example.employee.exception;

public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(String message) {
        super(message);
    }

    public DepartmentNotFoundException(Long id) {
        super("Department with ID " + id + " not found");
    }
}
