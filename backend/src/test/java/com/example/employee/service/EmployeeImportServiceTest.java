package com.example.employee.service;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.exception.DepartmentNotFoundException;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.service.impl.EmployeeImportServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeImportServiceTest {

    private static final String HEADER = "firstName,lastName,email,dateOfBirth,salary,joiningDate,department\n";
    private static final String VALID_ROW = "Ada,Lovelace,ada@example.com,1990-01-01,120000,2020-01-01,Engineering\n";
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final EmployeeService employeeService = mock(EmployeeService.class);
    private EmployeeImportServiceImpl importer;

    @BeforeEach
    void setUp() {
        importer = new EmployeeImportServiceImpl(employeeService, VALIDATOR_FACTORY.getValidator(), 5 * 1024 * 1024L);
    }

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void importsValidCsvThroughExistingEmployeeService() {
        var result = importer.importEmployees(csv("employees.csv", HEADER + VALID_ROW));

        assertEquals(1, result.totalRows());
        assertEquals(1, result.importedCount());
        assertEquals(0, result.rejectedCount());
        verify(employeeService).createEmployee(any(EmployeeRequestDto.class));
    }

    @Test
    void reportsBeanValidationErrorsPerRowAndContinues() {
        String invalid = "Ada,Lovelace,not-an-email,1990-01-01,-1,2020-01-01,Engineering\n";
        var result = importer.importEmployees(csv("employees.csv", HEADER + invalid + VALID_ROW));

        assertEquals(2, result.totalRows());
        assertEquals(1, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertEquals(2, result.rejectedRows().getFirst().rowNumber());
        assertTrue(result.rejectedRows().getFirst().errors().stream().anyMatch(error -> error.contains("email")));
        assertTrue(result.rejectedRows().getFirst().errors().stream().anyMatch(error -> error.contains("salary")));
        verify(employeeService, times(1)).createEmployee(any(EmployeeRequestDto.class));
    }

    @Test
    void rejectsDuplicateEmailWithinFile() {
        var result = importer.importEmployees(csv("employees.csv", HEADER + VALID_ROW + VALID_ROW));

        assertEquals(1, result.importedCount());
        assertEquals(1, result.rejectedCount());
        assertTrue(result.rejectedRows().getFirst().errors().getFirst().contains("duplicate email"));
        verify(employeeService, times(1)).createEmployee(any(EmployeeRequestDto.class));
    }

    @Test
    void reportsExistingDuplicateAndUnknownDepartment() {
        when(employeeService.createEmployee(any(EmployeeRequestDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"))
                .thenThrow(new DepartmentNotFoundException("Department not found"));
        String anotherEmail = "Grace,Hopper,grace@example.com,1990-01-01,120000,2020-01-01,Unknown\n";
        var result = importer.importEmployees(csv("employees.csv", HEADER + VALID_ROW + anotherEmail));

        assertEquals(0, result.importedCount());
        assertEquals(2, result.rejectedCount());
        assertTrue(result.rejectedRows().get(0).errors().getFirst().contains("Email already exists"));
        assertTrue(result.rejectedRows().get(1).errors().getFirst().contains("Department not found"));
    }

    @Test
    void mapsDepartmentIdToExistingCreateDto() {
        String idHeader = "firstName,lastName,email,dateOfBirth,salary,joiningDate,departmentId\n";
        var result = importer.importEmployees(csv("employees.csv", idHeader +
                "Ada,Lovelace,ada@example.com,1990-01-01,120000,2020-01-01,42\n"));

        assertEquals(1, result.importedCount());
        var captor = org.mockito.ArgumentCaptor.forClass(EmployeeRequestDto.class);
        verify(employeeService).createEmployee(captor.capture());
        assertEquals(42L, captor.getValue().getDepartmentId());
        assertNull(captor.getValue().getDepartment());
    }

    @Test
    void mapsTrimmedDepartmentNameToExistingCreateDto() {
        var result = importer.importEmployees(csv("employees.csv", HEADER +
                "Ada,Lovelace,ada@example.com,1990-01-01,120000,2020-01-01,  Engineering  \n"));

        assertEquals(1, result.importedCount());
        var captor = org.mockito.ArgumentCaptor.forClass(EmployeeRequestDto.class);
        verify(employeeService).createEmployee(captor.capture());
        assertEquals("Engineering", captor.getValue().getDepartment());
        assertNull(captor.getValue().getDepartmentId());
    }

    @Test
    void importsXlsxUsingHeaderRow() throws Exception {
        byte[] workbook = xlsx();
        var file = new MockMultipartFile("file", "employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook);

        var result = importer.importEmployees(file);

        assertEquals(1, result.importedCount());
        verify(employeeService).createEmployee(any(EmployeeRequestDto.class));
    }

    @Test
    void rejectsMalformedWorkbookBeforeWriting() {
        var file = new MockMultipartFile("file", "broken.xlsx", "application/octet-stream", "not a workbook".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> importer.importEmployees(file));
        verifyNoInteractions(employeeService);
    }

    @Test
    void rejectsMissingColumnsBeforeWriting() {
        var file = csv("employees.csv", "firstName,lastName,email\n" + "Ada,Lovelace,ada@example.com\n");
        var exception = assertThrows(IllegalArgumentException.class, () -> importer.importEmployees(file));
        assertTrue(exception.getMessage().contains("Missing required columns"));
        verifyNoInteractions(employeeService);
    }

    @Test
    void rejectsEmptyFileAndFileSizeLimit() {
        var empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> importer.importEmployees(empty));
        var tooLarge = new MockMultipartFile("file", "large.csv", "text/csv", new byte[5 * 1024 * 1024 + 1]);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> importer.importEmployees(tooLarge))
                .getMessage().contains("5 MB"));
        verifyNoInteractions(employeeService);
    }

    @Test
    void rejectsRowLimitBeforeWriting() {
        StringBuilder csv = new StringBuilder(HEADER);
        for (int i = 0; i < 1_001; i++) {
            csv.append("Ada,Lovelace,ada").append(i).append("@example.com,1990-01-01,120000,2020-01-01,Engineering\n");
        }
        var exception = assertThrows(IllegalArgumentException.class, () -> importer.importEmployees(csv("employees.csv", csv.toString())));
        assertTrue(exception.getMessage().contains("1,000"));
        verifyNoInteractions(employeeService);
    }

    @Test
    void headerOnlyFileReturnsZeroCounts() {
        var result = importer.importEmployees(csv("employees.csv", HEADER));
        assertEquals(0, result.totalRows());
        assertEquals(0, result.importedCount());
        assertEquals(0, result.rejectedCount());
        verifyNoInteractions(employeeService);
    }

    private static MockMultipartFile csv(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] xlsx() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Employees");
            String[] headers = {"firstName", "lastName", "email", "dateOfBirth", "salary", "joiningDate", "department"};
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            var data = sheet.createRow(1);
            String[] values = {"Ada", "Lovelace", "ada@example.com", "1990-01-01", "120000", "2020-01-01", "Engineering"};
            for (int i = 0; i < values.length; i++) data.createCell(i).setCellValue(values[i]);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
