# Employee Database – Employee Management System (EMS Pro)

A complete **Java Full Stack Development (FSD)** college-level project built with **Spring Boot**, **Spring Data JPA**, **Hibernate**, **MySQL**, and a responsive modern **Bootstrap 5 & Vanilla JS** frontend.

---

## 1. Project Overview

### Problem Statement
Managing employee records manually or across disparate spreadsheets is prone to data entry errors, accidental duplication, lack of role security, and slow retrieval. Organizations need a centralized, performant, and secure platform to store employee profiles, track departmental assignments, monitor employment statuses, and perform instant lookups.

### Objectives
- Provide a clean, intuitive, and responsive web dashboard for HR and Admin personnel.
- Implement a robust RESTful API following layered enterprise architecture.
- Prevent duplicate data (e.g., unique email constraint) with server and client-side validation.
- Provide real-time searching, departmental filtering, and employment status filtering.
- Persist all transactions directly into a relational MySQL database.
- Offer role-based control (**ADMIN** with full CRUD rights vs. **HR** with View/Add/Edit rights).

---

## 2. Technology Stack

### Backend
- **Language:** Java (JDK 21 / JDK 25 compatible)
- **Framework:** Spring Boot 3.3.4
- **Modules:**
  - Spring Web (REST API controllers)
  - Spring Data JPA (Data persistence & query derivation)
  - Hibernate ORM (Object-Relational Mapping)
  - Jakarta Bean Validation (Hibernate Validator)
- **Database Connector:** MySQL Connector/J (`com.mysql:mysql-connector-j`)
- **Build Tool:** Apache Maven 3.9.x

### Database
- **Engine:** MySQL Server 8.0
- **Demo Database Name:** `ems_pro_demo` (isolated demo profile; do not point the demo at `employee_db`)
- **Table:** `employees`

### Frontend
- **Markup:** HTML5 (Semantic elements)
- **Styling:** CSS3 & Bootstrap 5.3.3 + Bootstrap Icons
- **Logic:** Vanilla JavaScript (ES6+, Fetch API, async/await)
- **Typography:** Google Fonts (`Inter`)

### Architecture
- **Pattern:** Monolithic Layered Architecture
- **Layers:**
  $$\text{Frontend UI} \longrightarrow \text{REST Controller} \longrightarrow \text{Service Layer} \longrightarrow \text{Repository Layer} \longrightarrow \text{MySQL Database}$$

---

## 3. Project Directory Structure

```text
IP-EmployeeDatabaseProject/
├── backend/
│   ├── start-demo.ps1                              # Password-prompting isolated demo launcher
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/employee/
│   │   │   │   ├── EmployeeApplication.java        # Spring Boot Entry Point
│   │   │   │   ├── config/
│   │   │   │   │   ├── DataInitializer.java        # Auto-seeds 10 sample employees
│   │   │   │   │   └── WebConfig.java              # CORS & static resource mappings
│   │   │   │   ├── controller/
│   │   │   │   │   └── EmployeeController.java     # REST Endpoints
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ApiResponse.java            # Standard generic response
│   │   │   │   │   ├── EmployeeRequestDto.java     # Input DTO with Bean Validation
│   │   │   │   │   ├── EmployeeResponseDto.java    # Output DTO
│   │   │   │   │   └── ErrorResponse.java          # Error payload with field errors
│   │   │   │   ├── entity/
│   │   │   │   │   └── Employee.java               # JPA Entity mapping to MySQL
│   │   │   │   ├── exception/
│   │   │   │   │   ├── DuplicateEmailException.java
│   │   │   │   │   ├── EmployeeNotFoundException.java
│   │   │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice handler
│   │   │   │   ├── repository/
│   │   │   │   │   └── EmployeeRepository.java     # Spring Data JPA Repository
│   │   │   │   └── service/
│   │   │   │       ├── EmployeeService.java        # Business interface
│   │   │   │       └── impl/
│   │   │   │           └── EmployeeServiceImpl.java # Business logic implementation
│   │   │   └── resources/
│   │   │       ├── application.properties          # Port & MySQL configuration
│   │   │       └── static/                         # Frontend served directly by Spring Boot
│   │   │           ├── index.html
│   │   │           ├── employees.html
│   │   │           ├── add-employee.html
│   │   │           ├── css/style.css
│   │   │           └── js/app.js
│   │   └── test/
│   │       └── java/com/example/employee/
│   │           ├── controller/EmployeeControllerTest.java # WebMvcTest
│   │           └── service/EmployeeServiceTest.java       # Mockito unit tests
│   └── pom.xml
├── frontend/                                       # Standalone frontend copy
│   ├── index.html                                  # Dashboard with stats & tables
│   ├── employees.html                              # Personnel directory
│   ├── add-employee.html                           # Add/Edit employee form
│   ├── css/style.css
│   └── js/app.js
├── database/
│   ├── schema.sql                                  # DDL table creation script
│   └── data.sql                                    # Sample insert statements
├── README.md
└── MINI_PROJECT_REPORT_FORMAT.md                   # Mini project report structure and formatting guide
```

---

## 4. Database Schema Design

### Entity Relationship & Table Specification

The table `employees` in MySQL contains the following structure:

| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique employee identifier |
| `first_name` | `VARCHAR(50)` | `NOT NULL` | Employee given name |
| `last_name` | `VARCHAR(50)` | `NOT NULL` | Employee family name |
| `email` | `VARCHAR(100)` | `NOT NULL UNIQUE` | Unique email address |
| `phone` | `VARCHAR(20)` | `NULL` | Contact phone number |
| `date_of_birth`| `DATE` | `NULL` | Date of birth |
| `gender` | `VARCHAR(20)` | `NULL` | Gender (Male/Female/Other) |
| `department` | `VARCHAR(50)` | `NOT NULL` | IT, HR, Finance, Marketing, Sales, Operations |
| `designation` | `VARCHAR(100)` | `NULL` | Job designation |
| `salary` | `DECIMAL(12, 2)`| `NOT NULL, CHECK (salary > 0)` | Monthly compensation in ₹ |
| `joining_date` | `DATE` | `NOT NULL` | Employment start date |
| `address` | `VARCHAR(500)`| `NULL` | Full residential address |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'Active'` | `Active` or `Inactive` |
| `created_at` | `DATETIME` | `DEFAULT CURRENT_TIMESTAMP` | Record creation timestamp |
| `updated_at` | `DATETIME` | `ON UPDATE CURRENT_TIMESTAMP` | Last modified timestamp |

---

## 5. REST API Documentation

Base URL: `http://localhost:8081/api/employees`

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `GET` | `/api/employees` | Retrieve all employees (supports `?department=IT&status=Active`) | `200 OK` |
| `GET` | `/api/employees/{id}` | Retrieve single employee by ID | `200 OK`, `404 Not Found` |
| `POST` | `/api/employees` | Create a new employee profile | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PUT` | `/api/employees/{id}` | Update existing employee profile | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE`| `/api/employees/{id}` | Remove employee by ID | `200 OK`, `404 Not Found` |
| `GET` | `/api/employees/search?name={q}` | Search by first name, last name, or email | `200 OK` |
| `GET` | `/api/employees/department/{dept}` | Filter employees by department | `200 OK` |
| `GET` | `/api/employees/status/{status}` | Filter employees by status (`Active`/`Inactive`) | `200 OK` |
| `GET` | `/api/employees/stats` | Dashboard statistics (total, active, inactive) | `200 OK` |

### Sample JSON Request (POST / PUT)
```json
{
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul.sharma@example.com",
  "phone": "+91 98765 43210",
  "dateOfBirth": "1995-05-14",
  "gender": "Male",
  "department": "IT",
  "designation": "Senior Java Developer",
  "salary": 85000.00,
  "joiningDate": "2021-06-01",
  "address": "402 Tech Park, Hinjewadi, Pune",
  "status": "Active"
}
```

### Error Response Format
```json
{
  "message": "Validation failed for input data",
  "status": 400,
  "timestamp": "2026-09-27T20:39:29.376",
  "errors": [
    "firstName: First name is required",
    "salary: Salary must be greater than zero"
  ]
}
```

---

## 6. How to Run the Application

### Prerequisites
1. **Java JDK 21+** installed and available on `PATH`.
2. **Apache Maven 3.9.x** installed and available on `PATH`.
3. **MySQL Server 8.0** running locally on port 3306.
4. An empty demo schema and dedicated MySQL account provisioned as described below.

### Step 1: Provision an Isolated Demo Database
Sign in to MySQL as an administrator and create a database and account used only by the demo. Replace the password placeholder with a strong password you choose; do not commit or share it.
```sql
CREATE DATABASE ems_pro_demo
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'ems_demo'@'localhost' IDENTIFIED BY 'CHOOSE_A_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ems_pro_demo.* TO 'ems_demo'@'localhost';
```

If `ems_pro_demo` already exists, inspect it before using it. The application uses Flyway to create/update tables and seeds demo records only when this demo database is empty. Never configure the demo to use `employee_db` or a production database.

### Step 2: Start the Demo
Open PowerShell and run the launcher:
```powershell
cd "C:\path\to\IP-EmployeeDatabaseProject\backend"
.\start-demo.ps1
```
The launcher asks for the `ems_demo` password without displaying it, creates a fresh JWT secret for this run, selects the `demo` profile, and starts Spring Boot on port 8081. If Windows blocks the script, allow it for the current PowerShell window only:
```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\start-demo.ps1
```
Keep the PowerShell window open while using the application. Press `Ctrl+C` in that window to stop it. The seeded demo logins are `admin` / `Admin@123`, `hrmanager` / `Hr@123`, and `rahul.s` / `Emp@123`; use these only in the isolated demo database.

### Step 3: Access the Application
Open your web browser and navigate to:
- **Dashboard:** [http://localhost:8081/index.html](http://localhost:8081/index.html)
- **Personnel Directory:** [http://localhost:8081/employees.html](http://localhost:8081/employees.html)
- **Add Employee:** [http://localhost:8081/add-employee.html](http://localhost:8081/add-employee.html)
- **Leave Management:** [http://localhost:8081/leaves.html](http://localhost:8081/leaves.html)
- **REST API Endpoint:** [http://localhost:8081/api/employees](http://localhost:8081/api/employees)

Alternatively, the frontend files in `frontend/index.html` can be opened with VS Code Live Server or directly in any browser—they automatically interact with the backend API running on `http://localhost:8081`.

For the required mini project report sections and IP report font specifications, see [MINI_PROJECT_REPORT_FORMAT.md](MINI_PROJECT_REPORT_FORMAT.md).

---

## 7. Role-Based Access Control (RBAC) Demonstration

In accordance with Section 15 of the specifications, the system includes role switching between **ADMIN** and **HR**:
- **ADMIN:** Full permissions:
  - View employee list and profile modal
  - Add new employees
  - Edit/update existing profiles
  - Delete employees with modal confirmation
- **HR:**
  - View employee list and profile modal
  - Add new employees
  - Edit/update existing profiles
  - *Delete button is locked and restricted to ADMIN* with a visual lock icon tooltip.

Use the dropdown in the navigation bar to toggle between roles live during presentations.

---

## 8. Automated Testing

Run the automated test suite with Maven:
```powershell
cd backend
mvn test
```

### Included Tests:
- `EmployeeServiceTest.java`: 8 unit tests testing business validation, duplicate email rejection, employee not found scenarios, update integrity, search, and delete using Mockito.
- `EmployeeControllerTest.java`: 9 MockMvc integration tests verifying HTTP 200, 201, 400 validation errors, 404 not found, and 409 duplicate email conflict status codes.

---

## 9. Viva / Demonstration Walkthrough

1. **Dashboard & Metrics:** Open `http://localhost:8081/index.html`. Explain the stat cards showing Total, Active, Inactive employees and Department counts fetched via REST API.
2. **Search:** Type `Rahul` or `Manager` into the search box. The table updates instantly.
3. **Filter:** Select Department `IT` or Status `Inactive`. Explain how query parameters filter records without page reload.
4. **View Profile:** Click the **Eye icon** to view full profile details (Phone, Date of Birth, Salary, Joining Date, Residential Address).
5. **Add Employee:** Click `+ Add New Employee`, demonstrate client-side required field checks and email validation, then save. The record is inserted into MySQL.
6. **Duplicate Email Prevention:** Try adding another employee with the same email. The system catches the conflict and returns an informative HTTP 409 error banner.
7. **Edit Employee:** Click the **Pencil icon** on any employee, modify their designation or salary, and click `Update Employee`.
8. **Delete Employee:** Switch to `ADMIN` role, click the **Trash icon**, confirm in the modal, and watch the employee get cleanly deleted from MySQL and the table updated.
9. **Export Data:** Click `Export CSV` to download the current table as a spreadsheet.
