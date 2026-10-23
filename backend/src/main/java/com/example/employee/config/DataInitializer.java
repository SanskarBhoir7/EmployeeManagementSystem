package com.example.employee.config;

import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.entity.Role;
import com.example.employee.entity.User;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("demo")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // Step 1: Ensure initial standard departments exist
            Map<String, Department> deptMap = seedDepartmentsIfEmpty();

            // Step 2: Migrate any existing employee records with null department_id
            migrateExistingEmployees(deptMap);

            // Step 3: Seed sample employees if database has no employees
            if (employeeRepository.count() == 0) {
                logger.info("Database is empty. Seeding initial sample employee records with department relationships...");
                Department it = deptMap.getOrDefault("IT", getOrCreateDept("IT", "Information Technology"));
                Department hr = deptMap.getOrDefault("HR", getOrCreateDept("HR", "Human Resources"));
                Department finance = deptMap.getOrDefault("Finance", getOrCreateDept("Finance", "Finance and Payroll"));
                Department sales = deptMap.getOrDefault("Sales", getOrCreateDept("Sales", "Sales and Business Development"));
                Department marketing = deptMap.getOrDefault("Marketing", getOrCreateDept("Marketing", "Marketing and Communications"));
                Department operations = deptMap.getOrDefault("Operations", getOrCreateDept("Operations", "Operations and Logistics"));

                List<Employee> sampleEmployees = Arrays.asList(
                    new Employee(null, "Rahul", "Sharma", "rahul.sharma@example.com", "+91 98765 43210",
                            LocalDate.of(1995, 5, 14), "Male", it, "Senior Java Developer",
                            new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "402 Tech Park, Hinjewadi, Pune", "Active"),

                    new Employee(null, "Priya", "Patel", "priya.patel@example.com", "+91 98234 56781",
                            LocalDate.of(1994, 8, 22), "Female", hr, "HR Manager",
                            new BigDecimal("72000.00"), LocalDate.of(2020, 3, 15), "12 Green Avenue, Bandra, Mumbai", "Active"),

                    new Employee(null, "Amit", "Verma", "amit.verma@example.com", "+91 98111 22334",
                            LocalDate.of(1992, 11, 5), "Male", finance, "Senior Financial Analyst",
                            new BigDecimal("90000.00"), LocalDate.of(2019, 8, 10), "703 Silver Oak, Koramangala, Bengaluru", "Active"),

                    new Employee(null, "Sneha", "Kulkarni", "sneha.k@example.com", "+91 97654 32109",
                            LocalDate.of(1998, 2, 18), "Female", it, "Frontend Engineer",
                            new BigDecimal("65000.00"), LocalDate.of(2022, 1, 10), "88 Sapphire Heights, Kothrud, Pune", "Active"),

                    new Employee(null, "Vikram", "Singh", "vikram.singh@example.com", "+91 98980 12345",
                            LocalDate.of(1990, 7, 30), "Male", sales, "Regional Sales Lead",
                            new BigDecimal("78000.00"), LocalDate.of(2018, 11, 1), "15 MG Road, Connaught Place, New Delhi", "Active"),

                    new Employee(null, "Ananya", "Iyer", "ananya.iyer@example.com", "+91 99401 23456",
                            LocalDate.of(1996, 9, 12), "Female", marketing, "Digital Marketing Specialist",
                            new BigDecimal("60000.00"), LocalDate.of(2022, 7, 15), "24 Palm Grove, T Nagar, Chennai", "Active"),

                    new Employee(null, "Rohan", "Deshmukh", "rohan.d@example.com", "+91 97665 54433",
                            LocalDate.of(1993, 4, 25), "Male", operations, "Operations Supervisor",
                            new BigDecimal("58000.00"), LocalDate.of(2021, 9, 20), "304 Viman Nagar, Pune", "Active"),

                    new Employee(null, "Kavita", "Menon", "kavita.m@example.com", "+91 98840 98765",
                            LocalDate.of(1991, 12, 8), "Female", finance, "Payroll Executive",
                            new BigDecimal("52000.00"), LocalDate.of(2020, 2, 1), "10 Lake View, Whitefield, Bengaluru", "Inactive"),

                    new Employee(null, "Arjun", "Nair", "arjun.nair@example.com", "+91 98450 11223",
                            LocalDate.of(1997, 3, 19), "Male", sales, "Business Development Associate",
                            new BigDecimal("45000.00"), LocalDate.of(2023, 4, 1), "56 Marine Lines, Mumbai", "Inactive"),

                    new Employee(null, "Neha", "Gupta", "neha.gupta@example.com", "+91 98123 45670",
                            LocalDate.of(1995, 10, 3), "Female", hr, "Talent Acquisition Specialist",
                            new BigDecimal("55000.00"), LocalDate.of(2022, 10, 1), "18 Sector 14, Gurugram", "Active")
                );

                employeeRepository.saveAll(sampleEmployees);
                logger.info("Successfully seeded {} sample employees into MySQL employee_db.", sampleEmployees.size());
            }

            // Step 4: Seed initial user accounts
            if (userRepository.count() == 0) {
                logger.info("Seeding initial user accounts with encrypted BCrypt passwords...");
                User admin = new User(null, "admin", passwordEncoder.encode("Admin@123"), "admin@ems.local", Role.ADMIN, null, true);
                User hr = new User(null, "hrmanager", passwordEncoder.encode("Hr@123"), "hr@ems.local", Role.HR, null, true);
                User employee = new User(null, "rahul.s", passwordEncoder.encode("Emp@123"), "rahul.sharma@example.com", Role.EMPLOYEE, 1L, true);

                userRepository.saveAll(Arrays.asList(admin, hr, employee));
                logger.info("Successfully seeded 3 development accounts (ADMIN: admin, HR: hrmanager, EMPLOYEE: rahul.s linked to ID 1).");
            }
        } catch (Exception ex) {
            logger.warn("DataInitializer skipped or encountered an error: {}", ex.getMessage());
        }
    }

    private Map<String, Department> seedDepartmentsIfEmpty() {
        Map<String, Department> map = new HashMap<>();
        String[][] standardDepts = {
            {"IT", "Information Technology, software engineering, and infrastructure"},
            {"HR", "Human Resources, recruitment, and employee relations"},
            {"Finance", "Financial planning, accounting, and payroll"},
            {"Sales", "Corporate sales, client acquisition, and accounts"},
            {"Marketing", "Brand strategy, advertising, and digital marketing"},
            {"Operations", "Business operations, facilities, and logistics"}
        };

        for (String[] deptData : standardDepts) {
            String name = deptData[0];
            String desc = deptData[1];
            Department dept = departmentRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> departmentRepository.save(new Department(name, desc)));
            map.put(name.toUpperCase(), dept);
        }
        return map;
    }

    private Department getOrCreateDept(String name, String description) {
        return departmentRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> departmentRepository.save(new Department(name, description)));
    }

    private void migrateExistingEmployees(Map<String, Department> deptMap) {
        List<Employee> unmigrated = employeeRepository.findByDepartmentIsNull();
        if (unmigrated.isEmpty()) {
            return;
        }

        logger.info("Found {} unmigrated employee record(s). Migrating legacy text department to Department relationship...", unmigrated.size());
        for (Employee emp : unmigrated) {
            String legacyName = emp.getLegacyDepartment();
            Department dept = null;
            if (legacyName != null && !legacyName.trim().isEmpty()) {
                String cleanName = legacyName.trim();
                dept = deptMap.get(cleanName.toUpperCase());
                if (dept == null) {
                    dept = departmentRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> departmentRepository.save(new Department(cleanName, cleanName + " Department")));
                    deptMap.put(cleanName.toUpperCase(), dept);
                }
            } else {
                dept = deptMap.getOrDefault("IT", getOrCreateDept("IT", "Information Technology"));
            }
            emp.setDepartment(dept);
        }
        employeeRepository.saveAll(unmigrated);
        logger.info("Successfully migrated all {} employee records to Department entities.", unmigrated.size());
    }
}
