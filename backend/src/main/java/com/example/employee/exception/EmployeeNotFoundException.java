package com.example.employee.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public EmployeeNotFoundException(Long id) {
        super("Employee with ID " + id + " not found");
    }
}
