package com.example.employee.service;

import com.example.employee.dto.DepartmentRequestDto;
import com.example.employee.dto.DepartmentResponseDto;
import com.example.employee.entity.Department;
import com.example.employee.exception.DepartmentNotEmptyException;
import com.example.employee.exception.DepartmentNotFoundException;
import com.example.employee.exception.DuplicateDepartmentException;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department dept1;
    private Department dept2;

    @BeforeEach
    void setUp() {
        dept1 = new Department(1L, "IT", "Technology and Software");
        dept2 = new Department(2L, "HR", "People and Culture");
    }

    @Test
    @DisplayName("Should return all departments ordered by name with employee counts")
    void testGetAllDepartments() {
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dept2, dept1));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(10L);
        when(employeeRepository.countByDepartmentId(2L)).thenReturn(4L);

        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        assertEquals("HR", result.get(0).getName());
        assertEquals(4L, result.get(0).getEmployeeCount());
        assertEquals("IT", result.get(1).getName());
        assertEquals(10L, result.get(1).getEmployeeCount());
    }

    @Test
    @DisplayName("Should get department by ID successfully")
    void testGetDepartmentByIdSuccess() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(10L);

        DepartmentResponseDto result = departmentService.getDepartmentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("IT", result.getName());
        assertEquals(10L, result.getEmployeeCount());
    }

    @Test
    @DisplayName("Should throw DepartmentNotFoundException when department ID does not exist")
    void testGetDepartmentByIdNotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getDepartmentById(999L));
    }

    @Test
    @DisplayName("Should create department successfully with trimmed name")
    void testCreateDepartmentSuccess() {
        DepartmentRequestDto dto = new DepartmentRequestDto("  Finance  ", "Accounting & Audit");
        when(departmentRepository.existsByNameIgnoreCase("Finance")).thenReturn(false);
        Department saved = new Department(3L, "Finance", "Accounting & Audit");
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        DepartmentResponseDto result = departmentService.createDepartment(dto);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Finance", result.getName());
        assertEquals(0L, result.getEmployeeCount());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("Should reject blank name when creating department")
    void testCreateDepartmentBlankName() {
        DepartmentRequestDto dto = new DepartmentRequestDto("   ", "Description");

        assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(dto));
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should reject duplicate department name (case-insensitive) on creation")
    void testCreateDepartmentDuplicateName() {
        DepartmentRequestDto dto = new DepartmentRequestDto("it", "Information Technology");
        when(departmentRepository.existsByNameIgnoreCase("it")).thenReturn(true);

        assertThrows(DuplicateDepartmentException.class, () -> departmentService.createDepartment(dto));
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should update department successfully")
    void testUpdateDepartmentSuccess() {
        DepartmentRequestDto dto = new DepartmentRequestDto("Information Technology", "New Tech Desc");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Information Technology", 1L)).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(8L);

        DepartmentResponseDto result = departmentService.updateDepartment(1L, dto);

        assertNotNull(result);
        assertEquals("Information Technology", result.getName());
        assertEquals("New Tech Desc", result.getDescription());
        assertEquals(8L, result.getEmployeeCount());
    }

    @Test
    @DisplayName("Should reject duplicate department name (case-insensitive) on update")
    void testUpdateDepartmentDuplicateName() {
        DepartmentRequestDto dto = new DepartmentRequestDto("HR", "Duplicate HR");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("HR", 1L)).thenReturn(true);

        assertThrows(DuplicateDepartmentException.class, () -> departmentService.updateDepartment(1L, dto));
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Should delete department when employee count is 0")
    void testDeleteDepartmentSuccess() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(0L);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository, times(1)).delete(dept1);
    }

    @Test
    @DisplayName("Should throw DepartmentNotEmptyException when department has assigned employees")
    void testDeleteDepartmentNotEmpty() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept1));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(5L);

        DepartmentNotEmptyException ex = assertThrows(DepartmentNotEmptyException.class,
                () -> departmentService.deleteDepartment(1L));

        assertTrue(ex.getMessage().contains("5 employee(s) are assigned"));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    @Test
    @DisplayName("Should throw DepartmentNotFoundException when deleting non-existent department")
    void testDeleteDepartmentNotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.deleteDepartment(999L));
        verify(departmentRepository, never()).delete(any(Department.class));
    }
}
