# EMPLOYEE MANAGEMENT SYSTEM (EMS PRO)
## MINI PROJECT REPORT

**Submitted in partial fulfilment of the requirements for the Mini Project in Information Processing (IP)**

**Student Name:** [Enter student name]<br>
**Roll / Register Number:** [Enter number]<br>
**Class / Division:** [Enter class and division]<br>
**Department:** [Enter department]<br>
**Institute:** [Enter institute name]<br>
**Project Guide:** [Enter guide name]<br>
**Academic Year:** [Enter academic year]

## Formatting Instructions for Final Document

Apply the required IP report styling when converting this Markdown draft into Word or PDF:

| Report element | Font | Size | Style |
|---|---|---:|---|
| Main section headings (1–7) | Times New Roman (TNR) | 16 pt | Bold |
| Content headings and subheadings | Times New Roman (TNR) | 14 pt | Bold |
| Body paragraphs, captions, and references | Times New Roman (TNR) | 12 pt | Regular |

Markdown itself cannot enforce point sizes or font family. Keep the seven required sections in the specified order and apply these styles in the formatted report. The technical report text and code excerpts are drafted; only the personal-detail fields and project screenshots need to be supplied before submission.

## 1. Abstract

Employee information is central to routine organizational work, but maintaining that information through paper records or disconnected spreadsheets can make it difficult to keep records accurate, find information quickly, and control who may view or change it. This project presents EMS Pro, a web-based Employee Management System developed to organize employee records and related human-resources workflows in one application.

The system uses a Spring Boot backend, a MySQL relational database, and a browser-based interface built with HTML, CSS, Bootstrap, and JavaScript. It provides employee and department management, employee search and filtering, dashboard summaries, leave submission and review, user authentication, role-based authorization, and audit-log viewing for administrators. The application exposes REST endpoints between the interface and backend, validates submitted information, and persists records in the database.

The project was verified with an automated Maven test run that completed 170 tests with no failures or errors. During the isolated demo run, the application connected to the `ems_pro_demo` database, applied its database migrations, started on port 8081, and seeded sample employees and user accounts. The result is a working demonstration of how a layered web application can support common employee-management tasks with controlled access and persistent storage.

## 2. Introduction

### 2.1 Background

Organizations maintain information about employees throughout their employment, including contact details, department, designation, joining date, and employment status. Human-resources staff also handle related activities such as departmental organization, leave requests, and access to employee records. When these activities rely on separate files or manual processes, information can become inconsistent, repeated work increases, and it may be unclear who is authorized to make a change.

A web-based information system can provide a shared interface for these tasks while storing information in a structured database. EMS Pro applies this approach by combining employee records, departments, leave workflows, authentication, and reporting summaries in a single application.

### 2.2 Problem Statement

Manual or fragmented employee-record management makes it harder to maintain accurate, current information and to retrieve it efficiently. It can also provide insufficient control over sensitive records and leave decisions. The project addresses the need for a centralized application through which authorized users can manage employee information, review leave requests, and view relevant summaries, while employees can access their own permitted functions.

### 2.3 Aim and Objectives

The aim of the project is to design and implement a web-based system for managing employee information and selected HR workflows.

The project objectives are:

1. Maintain employee profiles with validation and persistent database storage.
2. Organize employees into departments and support search, filtering, and paginated views.
3. Present dashboard summaries of employee, department, and leave information.
4. Support leave request submission, review, approval, rejection, and cancellation rules.
5. Authenticate users and enforce permissions based on the ADMIN, HR, and EMPLOYEE roles.
6. Record important actions for administrative audit review.
7. Verify application behavior with automated tests and demonstrate the application using a separate demo database.

### 2.4 Scope

The project covers the application’s browser interface, REST API, business logic, relational persistence, and role-based access. Its primary areas are employee records, departments, leave requests, authentication, dashboard summaries, and audit logs. The demo configuration uses a dedicated `ems_pro_demo` schema and is intended for local demonstration. The project is an academic demonstration and is not presented as a production HR or payroll platform.

## 3. Project Description

### 3.1 Proposed System

EMS Pro provides a browser-based interface connected to a Java backend. Users sign in and receive functions according to their role. Administrators and HR users can work with organization-wide employee records and leave requests, subject to role-specific restrictions. Employees are limited to the functions and records permitted to their account. The system stores application data in MySQL and returns structured responses through REST APIs.

### 3.2 Technologies Used

| Area | Technology | Use in the project |
|---|---|---|
| Programming language | Java 21 source target | Backend application and business logic |
| Backend framework | Spring Boot 3.3.4 | Application configuration, REST services, and embedded server |
| Persistence | Spring Data JPA and Hibernate | Entity mapping and database access |
| Database | MySQL 8 | Relational storage for application records |
| Schema migration | Flyway | Versioned database migrations |
| Security | Spring Security and JSON Web Tokens | Authentication and endpoint authorization |
| Validation | Jakarta Bean Validation | Validation of API request data |
| Frontend | HTML5, CSS3, Bootstrap 5, JavaScript | Responsive user interface and API interactions |
| API documentation | Springdoc OpenAPI / Swagger UI | Interactive API documentation |
| Build and testing | Apache Maven, JUnit, Spring Boot Test | Dependency management, builds, and automated tests |

### 3.3 System Architecture

The application follows a layered architecture. The browser interface sends HTTP requests to REST controllers. Controllers validate and route requests to service classes, which apply business rules and use repository interfaces to access JPA entities. Hibernate translates persistence operations into SQL for MySQL. Responses are serialized as JSON and returned to the interface for display.

```text
Browser UI (HTML / CSS / JavaScript)
                 │ HTTP / JSON
                 ▼
       Spring Boot REST Controllers
                 ▼
       Service and Validation Layer
                 ▼
     Spring Data JPA Repositories
                 ▼
            MySQL Database
```

The frontend is packaged with the Spring Boot application and served from the same local server as the API. During the demo, the server listens on port 8081. Maven manages dependencies and the build; Flyway applies versioned schema changes at application startup.

### 3.4 Modules and Features

**Authentication and user management.** Users authenticate through the login endpoint. The backend issues a JSON Web Token used to access protected endpoints. Passwords are encoded before storage. Administrator-controlled registration and password-change functions are provided by the authentication module.

**Employee management.** Authorized users can create, view, and update employee profiles. Administrators can delete employee records. The employee information model includes identity and contact information, date of birth, department, designation, salary, joining date, address, employment status, and termination details. Input validation and duplicate-email handling help prevent invalid or conflicting records.

**Search, filtering, and pagination.** Employee records can be searched and filtered by supported criteria. The paginated endpoint accepts page, size, sort, and filter inputs and returns page metadata with the records, allowing the interface to request manageable result sets.

**Department management.** Departments provide an organizational grouping for employees. ADMIN and HR users can view department information, while department creation, update, and deletion are restricted to ADMIN.

**Leave management.** Authenticated users can submit leave requests and view their own requests. ADMIN and HR users can view organization-wide requests and review pending requests. Reviews set a request to APPROVED or REJECTED and store reviewer information and optional comments. Employees may cancel requests that remain pending. The interface presents request status and relevant leave details.

**Dashboard and analytics.** Dashboard cards summarize employee and department totals and leave activity. Recent leave activity is presented with employee, dates, reason, type, and status to support a quick review of current records.

**Audit logs.** The system records selected actions, including leave events, for traceability. Audit-log viewing is restricted to ADMIN and can be queried by entity type.

**Import and export.** The employee interface supports CSV export. The backend also includes a bulk employee-import endpoint with CSV and spreadsheet parsing support and upload size limits.

### 3.5 Database Design

The application uses a relational MySQL schema with the following primary tables:

| Table | Purpose |
|---|---|
| `employees` | Employee profile, employment, and contact data |
| `departments` | Department names and descriptions |
| `users` | Login credentials, role, and optional employee association |
| `leave_requests` | Employee leave dates, reason, status, and review details |
| `audit_logs` | Recorded actions and associated details |
| `flyway_schema_history_v2` | Applied database migration history |

An employee may be associated with a department. A user may be associated with an employee profile, enabling employee-specific access. Each leave request belongs to an employee. Audit records capture action and entity information. The schema uses primary keys, foreign keys, unique constraints, and validation constraints to maintain referential integrity and prevent duplicate values where required.

For demonstration, the project uses the separately provisioned `ems_pro_demo` database with the restricted-purpose `ems_demo` account. The demo startup checks that the datasource targets the expected schema before the application connects. Flyway creates and migrates tables in that schema; demo data initialization is enabled only under the `demo` profile.

### 3.6 Implementation and Security

The backend separates HTTP handling, business operations, and persistence. Request DTOs define the data accepted by the API, while response DTOs shape returned data. Validation failures and application exceptions are converted to structured HTTP responses. Role checks are enforced on protected controller endpoints in addition to role-aware interface behavior.

The demo database safeguard requires explicit database credentials and a JWT secret. The demo profile requires the schema name `ems_pro_demo`, and a startup guard refuses to connect to the protected `employee_db` schema. These protections are intended to keep the demonstration separate from an existing or production database. The demo launcher prompts for the database password and creates a fresh JWT secret for each run; credentials should not be committed to source control.

## 4. Results and Discussions

### 4.1 Dashboard Results

The dashboard presents organization-level employee and department metrics together with leave statistics and recent leave activity. In the captured demonstration screenshot, the dashboard displays 10 total employees, 8 active employees, no terminated employees, and 6 departments. It also displays two leave requests: one approved and one rejected, with no pending approvals. These values describe the sample data at the time of capture and may change after records are edited.

**Figure 1. EMS Pro dashboard showing employee, department, and leave summaries.**<br>
*[Insert a clear screenshot of the running dashboard here.]*

### 4.2 Employee Directory and Profile Management

The employee directory allows permitted users to review employee records and use search and filters to find relevant entries. Employee forms support profile creation and editing. Validation and duplicate-email handling provide feedback when submitted values do not meet application rules. The directory also supports CSV export.

**Figure 2. Employee directory with search and filtering controls.**<br>
*[Insert a screenshot of the employee directory here.]*

**Figure 3. Employee profile form or employee details view.**<br>
*[Insert a screenshot of the add/edit form or employee details modal here.]*

### 4.3 Leave Management

The leave workflow records the leave type, dates, reason, and decision status. An employee can review their own requests. HR and ADMIN users have a management view for organization-wide requests and can approve or reject requests that are pending. The dashboard’s recent activity table makes the resulting decisions visible alongside each request.

**Figure 4. Leave management page showing the management approvals view and leave statuses.**<br>
*[Insert a screenshot with the Management Approvals tab selected and the status filter set to All Statuses.]*

### 4.4 Authentication, Authorization, and Audit

After authentication, the interface exposes role-appropriate actions. The API independently enforces access rules: for example, employee deletion and audit-log viewing are administrator-only, while HR and ADMIN users can access organization-wide leave review functions. The audit module provides a record of selected actions for administrative review.

**Figure 5. EMS Pro navigation or screen demonstrating the signed-in role and role-specific actions.**<br>
*[Insert a screenshot of the authenticated interface here.]*

### 4.5 Verification and Discussion

The Maven test run completed with **170 tests, zero failures, zero errors, and zero skipped tests**. The test suite covers controller behavior, validation, role restrictions, employee services, leave services, import behavior, and demo database safety checks. The application was also started successfully in the demo profile against `ems_pro_demo`; Flyway applied two migrations and the initializer created sample employees and demo users in that isolated database.

The results demonstrate that the application’s primary flows operate across the interface, API, service, and database layers. The demo data supports repeatable presentation of employee records and leave decisions. The main scope limitation is that the system is an academic project: the supplied demo users and sample data are for local demonstration only, and the deployment has not been assessed as a production HR system.

## 5. Conclusion and Future Scope

### 5.1 Conclusion

EMS Pro demonstrates a full-stack approach to employee and selected HR record management. It combines a responsive web interface with a Spring Boot REST backend and MySQL persistence. The implemented modules cover employee profiles, departments, leave requests, authentication, role-based access, dashboard summaries, audit logs, and employee data import and export.

Separating the application into controller, service, and repository layers gives the project a clear structure for maintaining and extending its features. Input validation, database constraints, token-based authentication, and endpoint-level authorization provide basic safeguards for application data. Automated verification and an isolated demo database support repeatable testing and presentation. The project therefore meets its objective of providing a working demonstration of centralized employee management and leave workflows.

### 5.2 Future Scope

Possible future improvements include:

1. Add email or in-app notifications when leave requests are submitted or reviewed.
2. Provide a calendar view and configurable leave balances and policies.
3. Add richer analytics, downloadable reports, and configurable date ranges.
4. Expand audit history with improved filtering, retention, and export controls.
5. Add automated database backups and a documented recovery procedure.
6. Improve accessibility and add broader browser and responsive-layout testing.
7. Add integration tests against a disposable database and a repeatable deployment pipeline.
8. Replace demonstration credentials and local-only assumptions with managed configuration suitable for a production environment.

## 6. References

1. Spring, *Spring Boot Reference Documentation, version 3.3.4*. https://docs.spring.io/spring-boot/3.3.4/reference/
2. Spring, *Spring Data JPA Reference Documentation*. https://docs.spring.io/spring-data/jpa/reference/
3. Oracle, *Java Documentation*. https://docs.oracle.com/en/java/
4. Oracle, *MySQL 8.0 Reference Manual*. https://dev.mysql.com/doc/refman/8.0/en/
5. Flyway, *Flyway Documentation*. https://documentation.red-gate.com/flyway
6. Apache Software Foundation, *Maven Documentation*. https://maven.apache.org/guides/
7. Bootstrap Team, *Bootstrap 5.3 Documentation*. https://getbootstrap.com/docs/5.3/
8. Project source code and API documentation, *Employee Management System (EMS Pro)*, developed for the Information Processing mini project.

## 7. Annexure (Code)

The complete project source code is submitted with this report. The following excerpts show representative implementation details. They are shortened for readability; the complete files are available in the submitted project source tree.

| File | Purpose |
|---|---|
| `backend/src/main/java/com/example/employee/EmployeeApplication.java` | Spring Boot application entry point |
| `backend/src/main/java/com/example/employee/controller/EmployeeController.java` | Employee REST API and access rules |
| `backend/src/main/java/com/example/employee/service/impl/EmployeeServiceImpl.java` | Employee business operations |
| `backend/src/main/java/com/example/employee/controller/LeaveController.java` | Leave submission, listing, review, and cancellation API |
| `backend/src/main/java/com/example/employee/config/DemoDatabaseSafetyEnvironmentPostProcessor.java` | Startup checks for the isolated demo database |
| `backend/src/main/resources/db/migration/V1__initial_schema.sql` | Initial relational schema migration |
| `frontend/js/app.js` | Frontend API calls and interactive application behavior |

### Annexure A: Employee REST Controller Excerpt

**File:** `backend/src/main/java/com/example/employee/controller/EmployeeController.java`

The controller validates incoming employee data and delegates persistence and business rules to the service layer. This excerpt shows the create and list endpoints and the role restriction applied to them.

```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public ResponseEntity<EmployeeResponseDto> createEmployee(
        @Valid @RequestBody EmployeeRequestDto dto) {
    EmployeeResponseDto created = employeeService.createEmployee(dto);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
}

@GetMapping
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees(
        @RequestParam(required = false) String department,
        @RequestParam(required = false) String status) {
    if ((department != null && !department.isBlank())
            || (status != null && !status.isBlank())) {
        return ResponseEntity.ok(
                employeeService.filterEmployees(department, status));
    }
    return ResponseEntity.ok(employeeService.getAllEmployees());
}
```

### Annexure B: Leave Review Service Excerpt

**File:** `backend/src/main/java/com/example/employee/service/impl/LeaveServiceImpl.java`

This service method enforces the leave review rules. Only pending requests can be reviewed, the decision must be APPROVED or REJECTED, and a reviewer cannot review their own request. The service saves the decision and records an audit event.

```java
public LeaveResponseDto reviewLeave(
        Long id, LeaveReviewDto reviewDto, String reviewerUsername) {
    LeaveRequest leave = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new LeaveNotFoundException(id));

    if (leave.getStatus() != LeaveStatus.PENDING) {
        throw new InvalidLeaveStateException(
                "Only PENDING leave requests can be reviewed. Current status is "
                        + leave.getStatus() + ".");
    }

    if (reviewDto.getStatus() != LeaveStatus.APPROVED
            && reviewDto.getStatus() != LeaveStatus.REJECTED) {
        throw new IllegalArgumentException(
                "Review status must be either APPROVED or REJECTED.");
    }

    User reviewer = getUserByUsername(reviewerUsername);
    if (reviewer.getEmployeeId() != null
            && reviewer.getEmployeeId().equals(leave.getEmployee().getId())) {
        throw new IllegalStateException(
                "Reviewers are not permitted to review their own leave requests.");
    }

    leave.setStatus(reviewDto.getStatus());
    leave.setReviewedBy(reviewerUsername);
    leave.setReviewedAt(LocalDateTime.now());
    leave.setReviewComments(reviewDto.getReviewComments() == null
            ? null : reviewDto.getReviewComments().trim());

    LeaveRequest updated = leaveRequestRepository.save(leave);
    com.example.employee.entity.AuditAction action =
            reviewDto.getStatus() == LeaveStatus.APPROVED
                    ? com.example.employee.entity.AuditAction.LEAVE_APPROVED
                    : com.example.employee.entity.AuditAction.LEAVE_REJECTED;
    recordAudit(reviewerUsername, action, updated.getId(),
            "Leave request #" + updated.getId() + " "
                    + reviewDto.getStatus() + " by " + reviewerUsername);
    return LeaveResponseDto.fromEntity(updated);
}
```

### Annexure C: Database Schema Excerpt

**File:** `backend/src/main/resources/db/migration/V1__initial_schema.sql`

The initial migration creates relational tables and constraints. This excerpt illustrates the department and employee relationship, including a unique employee email, a positive-salary check, and a foreign key that preserves the employee record if a department is removed.

```sql
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department_id BIGINT,
    designation VARCHAR(100),
    salary DECIMAL(12, 2) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT chk_salary CHECK (salary > 0),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id)
        REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Annexure D: Demo Database Safety Check

**File:** `backend/src/main/java/com/example/employee/config/DemoDatabaseSafetyEnvironmentPostProcessor.java`

The startup guard checks the configured database name before a datasource or Flyway connection is created. In the demo profile, it requires the configured database name and JDBC URL to match `ems_pro_demo`, and it requires explicit credentials and a JWT secret.

```java
if (ORIGINAL_DATABASE.equalsIgnoreCase(databaseName)
        || ORIGINAL_DATABASE.equalsIgnoreCase(urlDatabase)) {
    throw new IllegalStateException(
            "Refusing to start against the protected employee_db database.");
}

if (environment.acceptsProfiles(Profiles.of("demo"))) {
    String demoDatabase = environment.getProperty(
            "app.demo.database-name", DEMO_DATABASE);
    if (databaseName == null || databaseName.isBlank()
            || !demoDatabase.equalsIgnoreCase(databaseName)
            || !demoDatabase.equalsIgnoreCase(urlDatabase)) {
        throw new IllegalStateException(
                "The demo profile requires DB_NAME and datasource URL to target "
                        + demoDatabase + ".");
    }
    requireEnvironmentValue(environment, "DB_USERNAME");
    requireEnvironmentValue(environment, "DB_PASSWORD");
    requireEnvironmentValue(environment, "JWT_SECRET");
}
```
