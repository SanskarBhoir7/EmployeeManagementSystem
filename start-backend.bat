@echo off
echo ======================================================================
echo Starting Employee Database Management System (Backend on port 8081)...
echo ======================================================================

set MAVEN_CMD="C:\Users\Sanskar Bhoir\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"

if exist %MAVEN_CMD% (
    cd backend
    %MAVEN_CMD% spring-boot:run
) else (
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        cd backend
        mvn spring-boot:run
    ) else (
        echo [ERROR] Maven not found. Please install Maven or configure PATH.
        pause
    )
)
pause
