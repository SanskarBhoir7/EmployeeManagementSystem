package com.example.employee.exception;

public class LeaveOverlapException extends RuntimeException {
    public LeaveOverlapException(String message) {
        super(message);
    }
}
