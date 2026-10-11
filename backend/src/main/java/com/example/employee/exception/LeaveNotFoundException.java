package com.example.employee.exception;

public class LeaveNotFoundException extends RuntimeException {
    public LeaveNotFoundException(String message) {
        super(message);
    }

    public LeaveNotFoundException(Long id) {
        super("Leave request with ID " + id + " not found");
    }
}
