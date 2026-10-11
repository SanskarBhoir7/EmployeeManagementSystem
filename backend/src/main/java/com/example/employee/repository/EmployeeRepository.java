package com.example.employee.repository;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Employee> searchEmployees(@Param("query") String query);

    @Query("SELECT e FROM Employee e WHERE " +
           "(e.department IS NOT NULL AND LOWER(e.department.name) = LOWER(:department)) OR " +
           "(e.legacyDepartment IS NOT NULL AND LOWER(e.legacyDepartment) = LOWER(:department))")
    List<Employee> findByDepartmentIgnoreCase(@Param("department") String department);

    List<Employee> findByStatus(EmploymentStatus status);

    @Query("SELECT e FROM Employee e WHERE " +
           "((e.department IS NOT NULL AND LOWER(e.department.name) = LOWER(:department)) OR " +
           "(e.legacyDepartment IS NOT NULL AND LOWER(e.legacyDepartment) = LOWER(:department))) AND " +
           "e.status = :status")
    List<Employee> findByDepartmentIgnoreCaseAndStatus(@Param("department") String department, @Param("status") EmploymentStatus status);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department IS NOT NULL AND e.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.department IS NOT NULL AND e.department.id = :departmentId")
    List<Employee> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.department IS NULL")
    List<Employee> findByDepartmentIsNull();
}
