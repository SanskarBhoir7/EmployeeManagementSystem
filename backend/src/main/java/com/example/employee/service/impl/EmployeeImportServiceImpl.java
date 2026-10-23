package com.example.employee.service.impl;

import com.example.employee.dto.EmployeeImportResponseDto;
import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.exception.DepartmentNotFoundException;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.service.EmployeeImportService;
import com.example.employee.service.EmployeeService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EmployeeImportServiceImpl implements EmployeeImportService {

    private static final int MAX_ROWS = 1_000;
    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "firstName", "lastName", "email", "dateOfBirth", "salary", "joiningDate");
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "firstName", "lastName", "email", "dateOfBirth", "salary", "joiningDate",
            "department", "departmentId", "phone", "gender", "designation", "address");

    private final EmployeeService employeeService;
    private final Validator validator;
    private final long maxFileBytes;

    public EmployeeImportServiceImpl(EmployeeService employeeService,
                                     Validator validator,
                                     @Value("${app.employee.import.max-file-size:5242880}") long maxFileBytes) {
        this.employeeService = employeeService;
        this.validator = validator;
        this.maxFileBytes = maxFileBytes;
    }

    @Override
    public EmployeeImportResponseDto importEmployees(MultipartFile file) {
        List<ParsedRow> rows = parseFile(file); // Parse and validate file structure before writing any row.
        List<EmployeeImportResponseDto.RejectedRow> rejected = new ArrayList<>();
        Set<String> importedEmails = new HashSet<>();
        int imported = 0;

        for (ParsedRow row : rows) {
            List<String> errors = new ArrayList<>(row.parseErrors());
            EmployeeRequestDto dto = row.dto();
            if (dto != null) {
                validator.validate(dto).stream()
                        .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                        .map(EmployeeImportServiceImpl::formatViolation)
                        .forEach(errors::add);

                String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase(Locale.ROOT);
                if (!email.isBlank() && importedEmails.contains(email)) {
                    errors.add("email: duplicate email in this import file");
                }
            }

            if (errors.isEmpty()) {
                try {
                    employeeService.createEmployee(dto);
                    importedEmails.add(dto.getEmail().trim().toLowerCase(Locale.ROOT));
                    imported++;
                } catch (DuplicateEmailException ex) {
                    errors.add("email: " + ex.getMessage());
                } catch (DepartmentNotFoundException ex) {
                    errors.add("department: " + ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    errors.add(ex.getMessage());
                } catch (DataIntegrityViolationException ex) {
                    errors.add("row violates a database constraint (for example, a duplicate email)");
                }
            }

            if (!errors.isEmpty()) {
                rejected.add(new EmployeeImportResponseDto.RejectedRow(row.rowNumber(), List.copyOf(errors)));
            }
        }

        return new EmployeeImportResponseDto(rows.size(), imported, rejected.size(), List.copyOf(rejected));
    }

    private List<ParsedRow> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload a non-empty .csv or .xlsx file.");
        }
        if (file.getSize() > maxFileBytes) {
            throw new IllegalArgumentException("Import file exceeds the 5 MB size limit.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("The uploaded file must have a .csv or .xlsx filename.");
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (lowerName.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
            throw new IllegalArgumentException("Only .csv and .xlsx files are supported.");
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof IllegalArgumentException argumentException) {
                throw argumentException;
            }
            throw new IllegalArgumentException("The uploaded file is malformed or could not be read.");
        }
    }

    private List<ParsedRow> parseCsv(MultipartFile file) throws IOException {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT)
                     .onUnmappableCharacter(CodingErrorAction.REPORT));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(false)
                     .build()
                     .parse(reader)) {
            Map<String, Integer> headers = validateHeaders(parser.getHeaderNames());
            List<ParsedRow> parsed = new ArrayList<>();
            int count = 0;
            for (CSVRecord record : parser) {
                if (++count > MAX_ROWS) {
                    throw new IllegalArgumentException("Import files may contain at most 1,000 rows after the header; blank rows count toward this limit.");
                }
                if (!record.isConsistent()) {
                    if (record.size() == 1 && record.get(0).isBlank()) continue;
                    throw new IllegalArgumentException("CSV row " + (record.getRecordNumber() + 1) + " has a different number of fields than the header.");
                }
                Map<String, String> values = new HashMap<>();
                for (var header : headers.entrySet()) {
                    values.put(header.getKey(), header.getValue() < record.size() ? record.get(header.getValue()).trim() : "");
                }
                if (!values.values().stream().allMatch(String::isBlank)) {
                    parsed.add(toRow(count + 1, values));
                }
            }
            return List.copyOf(parsed);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalArgumentException("CSV is malformed or is not valid UTF-8.");
        }
    }

    private List<ParsedRow> parseXlsx(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); Workbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() != 1) {
                throw new IllegalArgumentException("XLSX imports must contain exactly one worksheet.");
            }
            var sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("The first worksheet row must contain the column headers.");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<String> headerNames = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headerNames.add(formatter.formatCellValue(headerRow.getCell(i)).trim());
            }
            Map<String, Integer> headers = validateHeaders(headerNames);
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new IllegalArgumentException("Import files may contain at most 1,000 rows after the header; blank rows count toward this limit.");
            }

            List<ParsedRow> parsed = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null) continue;
                Map<String, String> values = new HashMap<>();
                for (var header : headers.entrySet()) {
                    var cell = excelRow.getCell(header.getValue());
                    String value;
                    if (("dateOfBirth".equals(header.getKey()) || "joiningDate".equals(header.getKey()))
                            && cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                            && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        value = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    } else {
                        value = formatter.formatCellValue(cell).trim();
                    }
                    values.put(header.getKey(), value);
                }
                if (!values.values().stream().allMatch(String::isBlank)) {
                    parsed.add(toRow(i + 1, values));
                }
            }
            return List.copyOf(parsed);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("The XLSX workbook is malformed or could not be read.");
        }
    }

    private Map<String, Integer> validateHeaders(List<String> rawHeaders) {
        if (rawHeaders == null || rawHeaders.isEmpty() || rawHeaders.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException("The file must have a header row.");
        }
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String header = rawHeaders.get(i) == null ? "" : rawHeaders.get(i).trim();
            if (i == 0 && header.startsWith("\uFEFF")) header = header.substring(1);
            String canonical = null;
            for (String allowed : ALLOWED_COLUMNS) {
                if (allowed.equalsIgnoreCase(header)) {
                    canonical = allowed;
                    break;
                }
            }
            if (canonical == null) {
                throw new IllegalArgumentException("Unsupported or blank column header: " + (header.isBlank() ? "(blank)" : header));
            }
            if (headers.putIfAbsent(canonical, i) != null) {
                throw new IllegalArgumentException("Duplicate column header: " + header);
            }
        }
        List<String> missing = REQUIRED_COLUMNS.stream().filter(required -> !headers.containsKey(required)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missing));
        }
        if (!headers.containsKey("department") && !headers.containsKey("departmentId")) {
            throw new IllegalArgumentException("Include either a department or departmentId column.");
        }
        return headers;
    }

    private ParsedRow toRow(int rowNumber, Map<String, String> values) {
        List<String> errors = new ArrayList<>();
        EmployeeRequestDto dto = new EmployeeRequestDto();
        dto.setFirstName(values.get("firstName"));
        dto.setLastName(values.get("lastName"));
        dto.setEmail(values.get("email"));
        dto.setPhone(emptyToNull(values.get("phone")));
        dto.setGender(emptyToNull(values.get("gender")));
        dto.setDepartment(emptyToNull(values.get("department")));
        dto.setDesignation(emptyToNull(values.get("designation")));
        dto.setAddress(emptyToNull(values.get("address")));

        String departmentId = values.get("departmentId");
        if (departmentId != null && !departmentId.isBlank()) {
            try {
                dto.setDepartmentId(Long.valueOf(departmentId.trim()));
            } catch (NumberFormatException ex) {
                errors.add("departmentId: must be a whole numeric department ID");
            }
        }
        if (dto.getDepartmentId() != null && dto.getDepartment() != null) {
            errors.add("department: provide department or departmentId, not both");
        }
        try {
            dto.setDateOfBirth(parseDate(values.get("dateOfBirth")));
        } catch (DateTimeParseException ex) {
            errors.add("dateOfBirth: use ISO format yyyy-MM-dd");
        }
        try {
            String salary = values.get("salary");
            dto.setSalary(salary == null || salary.isBlank() ? null : new BigDecimal(salary.trim()));
        } catch (NumberFormatException ex) {
            errors.add("salary: must be a valid decimal amount");
        }
        try {
            dto.setJoiningDate(parseDate(values.get("joiningDate")));
        } catch (DateTimeParseException ex) {
            errors.add("joiningDate: use ISO format yyyy-MM-dd");
        }
        return new ParsedRow(rowNumber, dto, errors);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String formatViolation(ConstraintViolation<EmployeeRequestDto> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private record ParsedRow(int rowNumber, EmployeeRequestDto dto, List<String> parseErrors) { }
}
