package com.example.employee.service;

import com.example.employee.dto.ChangePasswordRequestDto;
import com.example.employee.dto.LoginRequestDto;
import com.example.employee.dto.LoginResponseDto;
import com.example.employee.dto.RegisterUserRequestDto;
import com.example.employee.dto.UserProfileDto;
import com.example.employee.entity.Employee;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import com.example.employee.security.JwtTokenProvider;
import com.example.employee.security.UserPrincipal;
import com.example.employee.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    private User sampleUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 86400000L);
        authService = new AuthServiceImpl(authenticationManager, tokenProvider, userRepository, employeeRepository, passwordEncoder);

        sampleUser = new User(1L, "admin", "encodedHash", "admin@ems.local", Role.ADMIN, null, true);
        userPrincipal = UserPrincipal.create(sampleUser);
    }

    @Test
    @DisplayName("Should authenticate and return JWT token on valid credentials")
    void testLoginSuccess() {
        LoginRequestDto request = new LoginRequestDto("admin", "Admin@123");
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        LoginResponseDto response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(response.getToken().length() > 20);
        assertEquals("admin", response.getUsername());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid login password")
    void testLoginBadCredentials() {
        LoginRequestDto request = new LoginRequestDto("admin", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should register new user and hash password with BCrypt")
    void testRegisterUserSuccess() {
        RegisterUserRequestDto request = new RegisterUserRequestDto("newhr", "hr@test.com", "Password123", Role.HR, null);

        when(userRepository.existsByUsername("newhr")).thenReturn(false);
        when(userRepository.existsByEmail("hr@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("bcryptHashedValue");

        User savedUser = new User(2L, "newhr", "bcryptHashedValue", "hr@test.com", Role.HR, null, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileDto result = authService.registerUser(request);

        assertNotNull(result);
        assertEquals("newhr", result.getUsername());
        assertEquals(Role.HR, result.getRole());
        verify(passwordEncoder, times(1)).encode("Password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering duplicate username")
    void testRegisterUserDuplicateUsername() {
        RegisterUserRequestDto request = new RegisterUserRequestDto("admin", "admin@new.com", "Pass123", Role.HR, null);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when registering duplicate email")
    void testRegisterUserDuplicateEmail() {
        RegisterUserRequestDto request = new RegisterUserRequestDto("uniqueuser", "admin@ems.local", "Pass123", Role.HR, null);
        when(userRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@ems.local")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should change password successfully when current password matches")
    void testChangePasswordSuccess() {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("OldPassword", "NewPassword123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPassword", "encodedHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");

        assertDoesNotThrow(() -> authService.changePassword("admin", request));
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when current password does not match")
    void testChangePasswordWrongCurrentPassword() {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("WrongCurrent", "NewPassword123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongCurrent", "encodedHash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.changePassword("admin", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify BCryptPasswordEncoder generates standard BCrypt hash and matches correctly")
    void testBCryptPasswordEncoderHashFormat() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String raw = "Admin@123";
        String encoded = encoder.encode(raw);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("WrongPass", encoded));
    }
}

