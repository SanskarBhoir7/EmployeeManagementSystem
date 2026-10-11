package com.example.employee.exception;

public class InvalidLeaveStateException extends RuntimeException {
    public InvalidLeaveStateException(String message) {
        super(message);
    }
}
