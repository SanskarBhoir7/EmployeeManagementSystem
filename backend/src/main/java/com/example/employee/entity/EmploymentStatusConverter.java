package com.example.employee.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmploymentStatusConverter implements AttributeConverter<EmploymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(EmploymentStatus attribute) {
        return attribute != null ? attribute.name() : EmploymentStatus.ACTIVE.name();
    }

    @Override
    public EmploymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EmploymentStatus.ACTIVE;
        }
        try {
            return EmploymentStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EmploymentStatus.ACTIVE;
        }
    }
}
