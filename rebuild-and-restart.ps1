# Full rebuild and restart script
Write-Host "[*] Stopping application..." -ForegroundColor Yellow
& "$PSScriptRoot\stop-app.ps1"

Write-Host ""
Write-Host "[*] Cleaning build..." -ForegroundColor Yellow
& "$PSScriptRoot\gradlew.bat" clean

Write-Host ""
Write-Host "[*] Building with new code..." -ForegroundColor Yellow
& "$PSScriptRoot\gradlew.bat" build -x test

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[SUCCESS] Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "[*] Starting application..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\gradlew.bat bootRun"
    Write-Host ""
    Write-Host "[SUCCESS] Application is starting in a new window..." -ForegroundColor Green
    Write-Host "[INFO] Wait for 'Started NovelApplication' message" -ForegroundColor Cyan
    Write-Host "[INFO] Then test the crawl API again" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "[ERROR] Build failed! Check errors above." -ForegroundColor Red
}

