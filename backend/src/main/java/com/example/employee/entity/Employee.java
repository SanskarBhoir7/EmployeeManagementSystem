package com.example.employee.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employee_email", columnNames = "email")
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department", length = 50)
    private String legacyDepartment;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "address", length = 500)
    private String address;

    @Convert(converter = EmploymentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Employee() {
    }

    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    LocalDate dateOfBirth, String gender, String department, String designation,
                    BigDecimal salary, LocalDate joiningDate, String address, String status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.legacyDepartment = department;
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.address = address;
        if (status != null && !status.isBlank()) {
            try {
                this.status = EmploymentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                this.status = EmploymentStatus.ACTIVE;
            }
        } else {
            this.status = EmploymentStatus.ACTIVE;
        }
    }

    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    LocalDate dateOfBirth, String gender, String department, String designation,
                    BigDecimal salary, LocalDate joiningDate, String address, EmploymentStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.legacyDepartment = department;
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.address = address;
        this.status = status != null ? status : EmploymentStatus.ACTIVE;
    }

    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    LocalDate dateOfBirth, String gender, Department department, String designation,
                    BigDecimal salary, LocalDate joiningDate, String address, EmploymentStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.department = department;
        if (department != null) {
            this.legacyDepartment = department.getName();
        }
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.address = address;
        this.status = status != null ? status : EmploymentStatus.ACTIVE;
    }

    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    LocalDate dateOfBirth, String gender, Department department, String designation,
                    BigDecimal salary, LocalDate joiningDate, String address, String status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.department = department;
        if (department != null) {
            this.legacyDepartment = department.getName();
        }
        this.designation = designation;
        this.salary = salary;
        this.joiningDate = joiningDate;
        this.address = address;
        if (status != null && !status.isBlank()) {
            try {
                this.status = EmploymentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                this.status = EmploymentStatus.ACTIVE;
            }
        } else {
            this.status = EmploymentStatus.ACTIVE;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = EmploymentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        if (department != null) {
            this.legacyDepartment = department.getName();
        }
    }

    public String getLegacyDepartment() {
        return legacyDepartment;
    }

    public void setLegacyDepartment(String legacyDepartment) {
        this.legacyDepartment = legacyDepartment;
    }

    public String getDepartmentName() {
        if (this.department != null) {
            return this.department.getName();
        }
        return this.legacyDepartment;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public EmploymentStatus getStatus() {
        return status;
    }

    public void setStatus(EmploymentStatus status) {
        this.status = status != null ? status : EmploymentStatus.ACTIVE;
    }

    public void setStatus(String statusStr) {
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                this.status = EmploymentStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                this.status = EmploymentStatus.ACTIVE;
            }
        } else {
            this.status = EmploymentStatus.ACTIVE;
        }
    }

    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
