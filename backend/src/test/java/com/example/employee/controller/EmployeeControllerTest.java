package com.example.employee.controller;

import com.example.employee.dto.EmployeeRequestDto;
import com.example.employee.dto.EmployeeResponseDto;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmailException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmployeeResponseDto sampleResponseDto;
    private EmployeeRequestDto sampleRequestDto;

    @BeforeEach
    void setUp() {
        Employee emp = new Employee(1L, "Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", "Active");
        sampleResponseDto = EmployeeResponseDto.fromEntity(emp);

        sampleRequestDto = new EmployeeRequestDto("Rahul", "Sharma", "rahul.sharma@example.com", "9876543210",
                LocalDate.of(1995, 5, 14), "Male", "IT", "Developer",
                new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "Pune", "Active");
    }

    @Test
    @DisplayName("POST /api/employees - Should create employee and return 201")
    void testCreateEmployee() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("rahul.sharma@example.com"));
    }

    @Test
    @DisplayName("POST /api/employees - Should return 400 when validation fails (e.g. blank firstName)")
    void testCreateEmployeeValidationFailure() throws Exception {
        sampleRequestDto.setFirstName(""); // invalid

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/employees - Should return 409 when duplicate email exists")
    void testCreateEmployeeDuplicateEmail() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequestDto.class)))
                .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/employees - Should return 200 with list")
    void testGetAllEmployees() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList(sampleResponseDto));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Rahul"));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Should return 200 when exists")
    void testGetEmployeeByIdSuccess() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponseDto);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/employees/{id} - Should return 404 when not found")
    void testGetEmployeeByIdNotFound() throws Exception {
        when(employeeService.getEmployeeById(999L)).thenThrow(new EmployeeNotFoundException(999L));

        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} - Should return 200 on update")
    void testUpdateEmployee() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} - Should return 200 on delete")
    void testDeleteEmployee() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/employees/search?name=Rahul - Should return search results")
    void testSearchEmployees() throws Exception {
        when(employeeService.searchEmployees("Rahul")).thenReturn(Arrays.asList(sampleResponseDto));

        mockMvc.perform(get("/api/employees/search").param("name", "Rahul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Rahul"));
    }
}
