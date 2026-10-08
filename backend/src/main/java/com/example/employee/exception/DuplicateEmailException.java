package com.example.employee.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }

    public DuplicateEmailException(String email, Long existingId) {
        super("An employee with email '" + email + "' already exists (ID: " + existingId + ")");
    }
}
