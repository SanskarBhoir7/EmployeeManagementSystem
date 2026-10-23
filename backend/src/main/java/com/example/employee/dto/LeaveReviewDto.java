package com.example.employee.dto;

import com.example.employee.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A leave review decision and optional reviewer comments.")
public class LeaveReviewDto {

    @NotNull(message = "Review status is required")
    private LeaveStatus status; // APPROVED or REJECTED

    @Size(max = 500, message = "Review comments must not exceed 500 characters")
    private String reviewComments;

    public LeaveReviewDto() {
    }

    public LeaveReviewDto(LeaveStatus status, String reviewComments) {
        this.status = status;
        this.reviewComments = reviewComments;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }
}
