# PowerShell script to restart the Novel Backend application
# This script stops any running instances and starts a fresh one

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Novel Backend Application Restart Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop any running instances on port 8080
Write-Host "Step 1: Checking for processes on port 8080..." -ForegroundColor Yellow

$portCheck = netstat -ano | Select-String ":8080" | Select-String "LISTENING"
if ($portCheck) {
    Write-Host "Found process on port 8080. Attempting to stop..." -ForegroundColor Yellow

    $portCheck | ForEach-Object {
        $line = $_.Line
        $pid = ($line -split '\s+')[-1]

        if ($pid -match '^\d+$') {
            Write-Host "Killing process with PID: $pid" -ForegroundColor Red
            try {
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                Write-Host "Process $pid stopped successfully" -ForegroundColor Green
            } catch {
                Write-Host "Failed to stop process $pid" -ForegroundColor Red
            }
        }
    }

    Start-Sleep -Seconds 2
} else {
    Write-Host "No process found on port 8080" -ForegroundColor Green
}

# Step 2: Clean and build the project
Write-Host ""
Write-Host "Step 2: Cleaning and building the project..." -ForegroundColor Yellow
Write-Host "This may take a moment..." -ForegroundColor Gray

$buildOutput = & .\gradlew.bat clean build --console=plain 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
} else {
    Write-Host "Build failed! Check the output above." -ForegroundColor Red
    Write-Host ""
    Write-Host "Build output:" -ForegroundColor Yellow
    Write-Host $buildOutput
    exit 1
}

# Step 3: Start the application
Write-Host ""
Write-Host "Step 3: Starting the application..." -ForegroundColor Yellow
Write-Host "The application will run in this terminal window." -ForegroundColor Gray
Write-Host "Press Ctrl+C to stop the application." -ForegroundColor Gray
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Run the application
& .\gradlew.bat bootRun --console=plain

