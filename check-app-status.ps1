# Script to check application status and port usage
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Checking Application Status" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check port 8080
Write-Host "Checking port 8080..." -ForegroundColor Yellow
$portCheck = netstat -ano | Select-String ":8080" | Select-String "LISTENING"

if ($portCheck) {
    Write-Host "[X] Port 8080 is IN USE" -ForegroundColor Red
    Write-Host ""
    Write-Host "Processes using port 8080:" -ForegroundColor Yellow

    $portCheck | ForEach-Object {
        $line = $_.Line
        if ($line -match '\s+(\d+)\s*$') {
            $pid = $matches[1]
            try {
                $process = Get-Process -Id $pid -ErrorAction Stop
                Write-Host "  PID: $pid" -ForegroundColor White
                Write-Host "  Name: $($process.ProcessName)" -ForegroundColor White
                Write-Host "  Path: $($process.Path)" -ForegroundColor Gray
                Write-Host "  Start Time: $($process.StartTime)" -ForegroundColor Gray
                Write-Host ""
            } catch {
                Write-Host "  PID: $pid (Unable to get process details)" -ForegroundColor White
                Write-Host ""
            }
        }
    }
} else {
    Write-Host "[OK] Port 8080 is FREE" -ForegroundColor Green
}

Write-Host ""

# Check Java processes
Write-Host "Checking Java processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue

if ($javaProcesses) {
    Write-Host "Found Java processes:" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "  PID: $($_.Id)" -ForegroundColor White
        Write-Host "  CPU: $($_.CPU)" -ForegroundColor Gray
        Write-Host "  Memory: $([math]::Round($_.WorkingSet64 / 1MB, 2)) MB" -ForegroundColor Gray
        Write-Host "  Start Time: $($_.StartTime)" -ForegroundColor Gray

        # Get command line to identify novel-backend process
        try {
            $commandLine = (Get-WmiObject Win32_Process -Filter "ProcessId = $($_.Id)").CommandLine
            if ($commandLine -like "*novel*" -or $commandLine -like "*bootRun*") {
                Write-Host "  Command: $commandLine" -ForegroundColor Yellow
                Write-Host "  >>> THIS IS LIKELY THE NOVEL BACKEND <<<" -ForegroundColor Red
            } else {
                Write-Host "  Command: $commandLine" -ForegroundColor Gray
            }
        } catch {
            Write-Host "  (Unable to get command line)" -ForegroundColor Gray
        }
        Write-Host ""
    }
} else {
    Write-Host "[OK] No Java processes found" -ForegroundColor Green
}

Write-Host ""

# Check Gradle daemon
Write-Host "Checking Gradle daemon..." -ForegroundColor Yellow
$gradleDaemons = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    try {
        $cmd = (Get-WmiObject Win32_Process -Filter "ProcessId = $($_.Id)").CommandLine
        $cmd -like "*gradle*daemon*" -or $cmd -like "*GradleDaemon*"
    } catch {
        $false
    }
}

if ($gradleDaemons) {
    Write-Host "Found Gradle daemon processes:" -ForegroundColor Yellow
    $gradleDaemons | ForEach-Object {
        Write-Host "  PID: $($_.Id)" -ForegroundColor White
        Write-Host "  Memory: $([math]::Round($_.WorkingSet64 / 1MB, 2)) MB" -ForegroundColor Gray
    }
} else {
    Write-Host "[OK] No Gradle daemon processes found" -ForegroundColor Green
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Status check complete!" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

