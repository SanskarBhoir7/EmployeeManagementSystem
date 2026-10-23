package com.example.employee.dto;

import java.util.List;

/** Summary of a row-by-row employee import. Successful rows remain committed if another row is rejected. */
public record EmployeeImportResponseDto(
        int totalRows,
        int importedCount,
        int rejectedCount,
        List<RejectedRow> rejectedRows) {

    public record RejectedRow(int rowNumber, List<String> errors) { }
}
