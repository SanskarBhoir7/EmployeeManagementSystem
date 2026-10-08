-- ========================================================
-- Employee Database Project - Schema Definition
-- Database: employee_db
-- Author: Sanskar Bhoir
-- ========================================================

CREATE DATABASE IF NOT EXISTS employee_db;
USE employee_db;

-- Drop table if exists for fresh setups
-- DROP TABLE IF EXISTS employees;

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    department VARCHAR(50) NOT NULL,
    designation VARCHAR(100),
    salary DECIMAL(12, 2) NOT NULL,
    joining_date DATE NOT NULL,
    address VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT chk_salary CHECK (salary > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Useful indexes for performant searching and filtering
CREATE INDEX idx_employee_dept ON employees(department);
CREATE INDEX idx_employee_status ON employees(status);
CREATE INDEX idx_employee_name ON employees(first_name, last_name);
