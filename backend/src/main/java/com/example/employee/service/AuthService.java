package com.example.employee.service;

import com.example.employee.dto.ChangePasswordRequestDto;
import com.example.employee.dto.LoginRequestDto;
import com.example.employee.dto.LoginResponseDto;
import com.example.employee.dto.RegisterUserRequestDto;
import com.example.employee.dto.UserProfileDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    UserProfileDto getCurrentUserProfile(String username);

    UserProfileDto registerUser(RegisterUserRequestDto request);

    void changePassword(String username, ChangePasswordRequestDto request);
}
