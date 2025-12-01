@echo off
echo ========================================
echo Novel Backend Application Restart
echo ========================================
echo.

echo Step 1: Stopping any running processes on port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing process %%a
    taskkill /F /PID %%a 2>nul
)

echo.
echo Step 2: Building the application...
call gradlew.bat clean build
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Starting the application...
echo Press Ctrl+C to stop the application
echo.
call gradlew.bat bootRun

