package com.example.employee.service;

import com.example.employee.dto.LeaveRequestDto;
import com.example.employee.dto.LeaveResponseDto;
import com.example.employee.dto.LeaveReviewDto;
import com.example.employee.entity.LeaveStatus;

import java.util.List;

public interface LeaveService {

    LeaveResponseDto submitLeave(LeaveRequestDto dto, String authenticatedUsername);

    List<LeaveResponseDto> getMyLeaves(String authenticatedUsername);

    List<LeaveResponseDto> getAllLeaves(LeaveStatus statusFilter);

    LeaveResponseDto getLeaveById(Long id, String authenticatedUsername);

    LeaveResponseDto reviewLeave(Long id, LeaveReviewDto reviewDto, String reviewerUsername);

    LeaveResponseDto cancelLeave(Long id, String authenticatedUsername);
}
