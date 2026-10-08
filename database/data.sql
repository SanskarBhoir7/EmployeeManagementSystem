-- ========================================================
-- Employee Database Project - Sample Seed Data
-- ========================================================

USE employee_db;

INSERT INTO employees (first_name, last_name, email, phone, date_of_birth, gender, department, designation, salary, joining_date, address, status, created_at, updated_at)
VALUES
('Rahul', 'Sharma', 'rahul.sharma@example.com', '+91 98765 43210', '1995-05-14', 'Male', 'IT', 'Senior Java Developer', 85000.00, '2021-06-01', '402 Tech Park, Hinjewadi, Pune', 'Active', NOW(), NOW()),
('Priya', 'Patel', 'priya.patel@example.com', '+91 98234 56781', '1994-08-22', 'Female', 'HR', 'HR Manager', 72000.00, '2020-03-15', '12 Green Avenue, Bandra, Mumbai', 'Active', NOW(), NOW()),
('Amit', 'Verma', 'amit.verma@example.com', '+91 98111 22334', '1992-11-05', 'Male', 'Finance', 'Senior Financial Analyst', 90000.00, '2019-08-10', '703 Silver Oak, Koramangala, Bengaluru', 'Active', NOW(), NOW()),
('Sneha', 'Kulkarni', 'sneha.k@example.com', '+91 97654 32109', '1998-02-18', 'Female', 'IT', 'Frontend Engineer', 65000.00, '2022-01-10', '88 Sapphire Heights, Kothrud, Pune', 'Active', NOW(), NOW()),
('Vikram', 'Singh', 'vikram.singh@example.com', '+91 98980 12345', '1990-07-30', 'Male', 'Sales', 'Regional Sales Lead', 78000.00, '2018-11-01', '15 MG Road, Connaught Place, New Delhi', 'Active', NOW(), NOW()),
('Ananya', 'Iyer', 'ananya.iyer@example.com', '+91 99401 23456', '1996-09-12', 'Female', 'Marketing', 'Digital Marketing Specialist', 60000.00, '2022-07-15', '24 Palm Grove, T Nagar, Chennai', 'Active', NOW(), NOW()),
('Rohan', 'Deshmukh', 'rohan.d@example.com', '+91 97665 54433', '1993-04-25', 'Male', 'Operations', 'Operations Supervisor', 58000.00, '2021-09-20', '304 Viman Nagar, Pune', 'Active', NOW(), NOW()),
('Kavita', 'Menon', 'kavita.m@example.com', '+91 98840 98765', '1991-12-08', 'Female', 'Finance', 'Payroll Executive', 52000.00, '2020-02-01', '10 Lake View, Whitefield, Bengaluru', 'Inactive', NOW(), NOW()),
('Arjun', 'Nair', 'arjun.nair@example.com', '+91 98450 11223', '1997-03-19', 'Male', 'Sales', 'Business Development Associate', 45000.00, '2023-04-01', '56 Marine Lines, Mumbai', 'Inactive', NOW(), NOW()),
('Neha', 'Gupta', 'neha.gupta@example.com', '+91 98123 45670', '1995-10-03', 'Female', 'HR', 'Talent Acquisition Specialist', 55000.00, '2022-10-01', '18 Sector 14, Gurugram', 'Active', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
