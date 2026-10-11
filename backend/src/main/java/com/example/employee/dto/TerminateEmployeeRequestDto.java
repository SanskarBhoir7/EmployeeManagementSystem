package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TerminateEmployeeRequestDto {

    @NotNull(message = "Termination effective date is required")
    private LocalDate effectiveDate;

    @NotBlank(message = "Termination reason is required")
    @Size(min = 3, max = 500, message = "Termination reason must be between 3 and 500 characters")
    private String reason;

    public TerminateEmployeeRequestDto() {
    }

    public TerminateEmployeeRequestDto(LocalDate effectiveDate, String reason) {
        this.effectiveDate = effectiveDate;
        this.reason = reason;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
