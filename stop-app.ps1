# Quick stop script for Novel Backend application
# Stops any process using port 8080

Write-Host "Stopping Novel Backend application..." -ForegroundColor Yellow

$portCheck = netstat -ano | Select-String ":8080" | Select-String "LISTENING"

if ($portCheck) {
    $portCheck | ForEach-Object {
        $line = $_.Line
        $pid = ($line -split '\s+')[-1]

        if ($pid -match '^\d+$') {
            Write-Host "Killing process with PID: $pid" -ForegroundColor Red
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
            Write-Host "Process stopped" -ForegroundColor Green
        }
    }
} else {
    Write-Host "No application running on port 8080" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green

