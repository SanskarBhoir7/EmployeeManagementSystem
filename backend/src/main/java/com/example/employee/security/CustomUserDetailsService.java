package com.example.employee.security;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));

        boolean enabled = user.isEnabled();
        if (enabled && user.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(user.getEmployeeId()).orElse(null);
            if (employee != null && employee.getStatus() == EmploymentStatus.TERMINATED) {
                enabled = false;
            }
        } else if (enabled && user.getRole() == Role.EMPLOYEE) {
            Employee employee = employeeRepository.findByEmail(user.getEmail()).orElse(null);
            if (employee != null && employee.getStatus() == EmploymentStatus.TERMINATED) {
                enabled = false;
            }
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRole(),
                user.getEmployeeId(),
                enabled
        );
    }
}
