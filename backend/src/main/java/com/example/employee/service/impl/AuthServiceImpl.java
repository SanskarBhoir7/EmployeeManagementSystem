package com.example.employee.service.impl;

import com.example.employee.dto.ChangePasswordRequestDto;
import com.example.employee.dto.LoginRequestDto;
import com.example.employee.dto.LoginResponseDto;
import com.example.employee.dto.RegisterUserRequestDto;
import com.example.employee.dto.UserProfileDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.User;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.UserPrincipal;
import com.example.employee.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return new LoginResponseDto(
                token,
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getRole(),
                userPrincipal.getEmployeeId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        String employeeName = null;
        if (user.getEmployeeId() != null) {
            employeeName = employeeRepository.findById(user.getEmployeeId())
                    .map(e -> e.getFirstName() + " " + e.getLastName())
                    .orElse(null);
        }

        return UserProfileDto.fromEntity(user, employeeName);
    }

    @Override
    public UserProfileDto registerUser(RegisterUserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email '" + request.getEmail() + "' is already in use by another user account.");
        }

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee with ID " + request.getEmployeeId() + " does not exist."));
            if (userRepository.findByEmployeeId(request.getEmployeeId()).isPresent()) {
                throw new IllegalArgumentException("Employee ID " + request.getEmployeeId() + " is already linked to another user account.");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt hashed
        user.setRole(request.getRole());
        user.setEmployeeId(request.getEmployeeId());
        user.setEnabled(true);

        User saved = userRepository.save(user);

        String employeeName = null;
        if (saved.getEmployeeId() != null) {
            employeeName = employeeRepository.findById(saved.getEmployeeId())
                    .map(e -> e.getFirstName() + " " + e.getLastName())
                    .orElse(null);
        }

        return UserProfileDto.fromEntity(saved, employeeName);
    }

    @Override
    public void changePassword(String username, ChangePasswordRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password does not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
