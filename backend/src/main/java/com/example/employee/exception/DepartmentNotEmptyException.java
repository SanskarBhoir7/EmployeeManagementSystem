package com.example.employee.exception;

public class DepartmentNotEmptyException extends RuntimeException {
    public DepartmentNotEmptyException(String message) {
        super(message);
    }
}
