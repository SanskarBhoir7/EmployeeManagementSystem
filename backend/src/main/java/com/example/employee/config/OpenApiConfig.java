package com.example.employee.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "EMS Pro Employee Management API",
        version = "1.0.0",
        description = "REST API for employee, department, leave, audit, and authentication workflows. " +
                "Protected endpoints use the Bearer JWT returned by POST /api/auth/login."
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter the JWT value returned by POST /api/auth/login. Swagger UI adds the Bearer prefix."
)
public class OpenApiConfig {
}
