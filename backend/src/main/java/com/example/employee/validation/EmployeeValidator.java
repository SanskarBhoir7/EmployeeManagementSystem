package com.example.employee.validation;

import com.example.employee.dto.EmployeeRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EmployeeValidator {

    private final int minEmploymentAge;

    public EmployeeValidator(@Value("${app.employee.min-age:18}") int minEmploymentAge) {
        this.minEmploymentAge = minEmploymentAge;
    }

    public int getMinEmploymentAge() {
        return minEmploymentAge;
    }

    /**
     * Validates employee business rules:
     * - Name cannot be blank or contain only whitespace
     * - DOB must not be in the future
     * - DOJ must be strictly after DOB
     * - DOJ must satisfy the configurable minimum employment age rule (doj >= dob + minEmploymentAge)
     * - Historical DOJ is valid if above rules hold; future DOJ is permitted for planned hires.
     */
    public void validate(EmployeeRequestDto dto) {
        if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be blank or contain only whitespace");
        }
        if (dto.getLastName() == null || dto.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be blank or contain only whitespace");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (dto.getDepartmentId() == null && (dto.getDepartment() == null || dto.getDepartment().trim().isEmpty())) {
            throw new IllegalArgumentException("Department is required");
        }

        LocalDate dob = dto.getDateOfBirth();
        LocalDate doj = dto.getJoiningDate();
        LocalDate today = LocalDate.now();

        if (dob == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (doj == null) {
            throw new IllegalArgumentException("Joining date is required");
        }

        // DOB must not be in the future
        if (dob.isAfter(today)) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }

        // DOJ must be strictly after DOB
        if (!doj.isAfter(dob)) {
            throw new IllegalArgumentException("Joining date must be strictly after date of birth");
        }

        // Minimum age rule: DOJ must satisfy minimum employment age
        LocalDate minAgeDateAtJoining = dob.plusYears(minEmploymentAge);
        if (doj.isBefore(minAgeDateAtJoining)) {
            throw new IllegalArgumentException(
                String.format("Employee must be at least %d years old on their joining date (minimum allowable joining date for DOB %s is %s)",
                    minEmploymentAge, dob, minAgeDateAtJoining)
            );
        }
    }
}
