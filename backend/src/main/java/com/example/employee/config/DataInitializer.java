package com.example.employee.config;

import com.example.employee.entity.Employee;
import com.example.employee.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final EmployeeRepository repository;

    @Autowired
    public DataInitializer(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        try {
            if (repository.count() == 0) {
                logger.info("Database is empty. Seeding initial sample employee records...");
                List<Employee> sampleEmployees = Arrays.asList(
                    new Employee(null, "Rahul", "Sharma", "rahul.sharma@example.com", "+91 98765 43210",
                            LocalDate.of(1995, 5, 14), "Male", "IT", "Senior Java Developer",
                            new BigDecimal("85000.00"), LocalDate.of(2021, 6, 1), "402 Tech Park, Hinjewadi, Pune", "Active"),

                    new Employee(null, "Priya", "Patel", "priya.patel@example.com", "+91 98234 56781",
                            LocalDate.of(1994, 8, 22), "Female", "HR", "HR Manager",
                            new BigDecimal("72000.00"), LocalDate.of(2020, 3, 15), "12 Green Avenue, Bandra, Mumbai", "Active"),

                    new Employee(null, "Amit", "Verma", "amit.verma@example.com", "+91 98111 22334",
                            LocalDate.of(1992, 11, 5), "Male", "Finance", "Senior Financial Analyst",
                            new BigDecimal("90000.00"), LocalDate.of(2019, 8, 10), "703 Silver Oak, Koramangala, Bengaluru", "Active"),

                    new Employee(null, "Sneha", "Kulkarni", "sneha.k@example.com", "+91 97654 32109",
                            LocalDate.of(1998, 2, 18), "Female", "IT", "Frontend Engineer",
                            new BigDecimal("65000.00"), LocalDate.of(2022, 1, 10), "88 Sapphire Heights, Kothrud, Pune", "Active"),

                    new Employee(null, "Vikram", "Singh", "vikram.singh@example.com", "+91 98980 12345",
                            LocalDate.of(1990, 7, 30), "Male", "Sales", "Regional Sales Lead",
                            new BigDecimal("78000.00"), LocalDate.of(2018, 11, 1), "15 MG Road, Connaught Place, New Delhi", "Active"),

                    new Employee(null, "Ananya", "Iyer", "ananya.iyer@example.com", "+91 99401 23456",
                            LocalDate.of(1996, 9, 12), "Female", "Marketing", "Digital Marketing Specialist",
                            new BigDecimal("60000.00"), LocalDate.of(2022, 7, 15), "24 Palm Grove, T Nagar, Chennai", "Active"),

                    new Employee(null, "Rohan", "Deshmukh", "rohan.d@example.com", "+91 97665 54433",
                            LocalDate.of(1993, 4, 25), "Male", "Operations", "Operations Supervisor",
                            new BigDecimal("58000.00"), LocalDate.of(2021, 9, 20), "304 Viman Nagar, Pune", "Active"),

                    new Employee(null, "Kavita", "Menon", "kavita.m@example.com", "+91 98840 98765",
                            LocalDate.of(1991, 12, 8), "Female", "Finance", "Payroll Executive",
                            new BigDecimal("52000.00"), LocalDate.of(2020, 2, 1), "10 Lake View, Whitefield, Bengaluru", "Inactive"),

                    new Employee(null, "Arjun", "Nair", "arjun.nair@example.com", "+91 98450 11223",
                            LocalDate.of(1997, 3, 19), "Male", "Sales", "Business Development Associate",
                            new BigDecimal("45000.00"), LocalDate.of(2023, 4, 1), "56 Marine Lines, Mumbai", "Inactive"),

                    new Employee(null, "Neha", "Gupta", "neha.gupta@example.com", "+91 98123 45670",
                            LocalDate.of(1995, 10, 3), "Female", "HR", "Talent Acquisition Specialist",
                            new BigDecimal("55000.00"), LocalDate.of(2022, 10, 1), "18 Sector 14, Gurugram", "Active")
                );

                repository.saveAll(sampleEmployees);
                logger.info("Successfully seeded {} sample employees into MySQL employee_db.", sampleEmployees.size());
            }
        } catch (Exception ex) {
            logger.warn("DataInitializer skipped or encountered an error: {}", ex.getMessage());
        }
    }
}
