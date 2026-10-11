package com.example.employee.validation;

import com.example.employee.dto.EmployeeRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeValidatorTest {

    private EmployeeValidator validator;
    private EmployeeRequestDto validDto;

    @BeforeEach
    void setUp() {
        validator = new EmployeeValidator(18); // Configurable minimum age = 18

        validDto = new EmployeeRequestDto(
                "Aman", "Gupta", "aman.g@example.com", "+91 98765 43210",
                LocalDate.of(1995, 5, 20), "Male", "Engineering", "Developer",
                new BigDecimal("75000.00"), LocalDate.of(2020, 6, 1), "Delhi", "Active"
        );
    }

    @Test
    @DisplayName("Valid DTO with minimum age satisfied should pass validation")
    void testValidDtoPasses() {
        assertDoesNotThrow(() -> validator.validate(validDto));
    }

    @Test
    @DisplayName("Future DOB should be rejected")
    void testFutureDobRejected() {
        validDto.setDateOfBirth(LocalDate.now().plusDays(1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex.getMessage().contains("Date of birth cannot be in the future"));
    }

    @Test
    @DisplayName("DOJ before DOB should be rejected")
    void testDojBeforeDobRejected() {
        validDto.setDateOfBirth(LocalDate.of(2000, 1, 1));
        validDto.setJoiningDate(LocalDate.of(1999, 12, 31));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex.getMessage().contains("Joining date must be strictly after date of birth"));
    }

    @Test
    @DisplayName("DOJ equal to DOB should be rejected")
    void testDojEqualToDobRejected() {
        validDto.setDateOfBirth(LocalDate.of(2000, 1, 1));
        validDto.setJoiningDate(LocalDate.of(2000, 1, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex.getMessage().contains("Joining date must be strictly after date of birth"));
    }

    @Test
    @DisplayName("DOJ satisfying less than configurable minimum age (18) should be rejected")
    void testUnderMinimumAgeRejected() {
        // Born 2005-01-01, Joining 2022-01-01 (17 years old)
        validDto.setDateOfBirth(LocalDate.of(2005, 1, 1));
        validDto.setJoiningDate(LocalDate.of(2022, 1, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex.getMessage().contains("Employee must be at least 18 years old on their joining date"));
    }

    @Test
    @DisplayName("Valid historical joining date satisfying minimum age should be accepted")
    void testValidHistoricalJoiningDateAccepted() {
        // Born 1990-01-01, Joined in 2012 (historical)
        validDto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        validDto.setJoiningDate(LocalDate.of(2012, 8, 15));

        assertDoesNotThrow(() -> validator.validate(validDto));
    }

    @Test
    @DisplayName("Planned future joining date satisfying minimum age should be accepted")
    void testPlannedFutureJoiningDateAccepted() {
        // Born 1998-03-10, Joining in 6 months
        validDto.setDateOfBirth(LocalDate.of(1998, 3, 10));
        validDto.setJoiningDate(LocalDate.now().plusMonths(6));

        assertDoesNotThrow(() -> validator.validate(validDto));
    }

    @Test
    @DisplayName("Blank first name or last name should be rejected")
    void testBlankNamesRejected() {
        validDto.setFirstName("   ");
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex1.getMessage().contains("First name cannot be blank"));

        validDto.setFirstName("Aman");
        validDto.setLastName("   ");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex2.getMessage().contains("Last name cannot be blank"));
    }

    @Test
    @DisplayName("Blank email should be rejected")
    void testBlankEmailRejected() {
        validDto.setEmail("   ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(validDto));
        assertTrue(ex.getMessage().contains("Email is required"));
    }

    @Test
    @DisplayName("Configurable minimum age check (e.g. 21 years)")
    void testCustomConfigurableMinimumAge() {
        EmployeeValidator customValidator = new EmployeeValidator(21);
        assertEquals(21, customValidator.getMinEmploymentAge());

        // Born 2002-01-01, Joining 2021-01-01 (19 years old - valid for 18, invalid for 21)
        validDto.setDateOfBirth(LocalDate.of(2002, 1, 1));
        validDto.setJoiningDate(LocalDate.of(2021, 1, 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> customValidator.validate(validDto));
        assertTrue(ex.getMessage().contains("Employee must be at least 21 years old on their joining date"));
    }
}
