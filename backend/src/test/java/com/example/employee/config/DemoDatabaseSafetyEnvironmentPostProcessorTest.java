package com.example.employee.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemoDatabaseSafetyEnvironmentPostProcessorTest {

    private final DemoDatabaseSafetyEnvironmentPostProcessor processor =
            new DemoDatabaseSafetyEnvironmentPostProcessor();

    @Test
    void permitsOnlyExplicitDemoDatabaseAndCredentials() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("demo");
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DB_NAME", "ems_pro_demo",
                "DB_USERNAME", "demo_user",
                "DB_PASSWORD", "test-only-secret",
                "JWT_SECRET", "test-only-jwt-secret-long-enough-for-hmac-sha256",
                "spring.datasource.url", "jdbc:mysql://localhost:3306/ems_pro_demo?useSSL=false")));

        assertDoesNotThrow(() -> processor.postProcessEnvironment(environment, new SpringApplication()));
    }

    @Test
    void rejectsOriginalDatabaseBeforeStartup() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("demo");
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DB_NAME", "employee_db",
                "spring.datasource.url", "jdbc:mysql://localhost:3306/employee_db?useSSL=false")));

        assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, new SpringApplication()));
    }
}
