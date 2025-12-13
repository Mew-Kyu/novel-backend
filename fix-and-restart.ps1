# Fix Spring Boot 3.5.7 + springdoc compatibility issue
# This will update dependencies and rebuild

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Fixing springdoc Version Compatibility" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Issue: Spring Boot 3.5.7 + springdoc-openapi 2.3.0 = INCOMPATIBLE" -ForegroundColor Yellow
Write-Host "Fix: Updating to springdoc-openapi 2.7.0" -ForegroundColor Green
Write-Host ""

Write-Host "[1/3] Cleaning previous build..." -ForegroundColor Yellow
& .\gradlew.bat clean

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Clean failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n[2/3] Downloading updated dependencies and building..." -ForegroundColor Yellow
Write-Host "       This will download springdoc-openapi 2.7.0..." -ForegroundColor Gray
& .\gradlew.bat build -x test

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "If you see compilation errors, they might be due to:" -ForegroundColor Yellow
    Write-Host "  - Network issues downloading dependencies" -ForegroundColor Gray
    Write-Host "  - Gradle cache corruption" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Try:" -ForegroundColor Cyan
    Write-Host "  .\gradlew.bat clean build --refresh-dependencies" -ForegroundColor White
    exit 1
}

Write-Host "`n[3/3] Starting backend with updated dependencies..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Watch for successful startup message:" -ForegroundColor Cyan
Write-Host "  'Started NovelApplication in X.XXX seconds'" -ForegroundColor Green
Write-Host ""
Write-Host "Then test OpenAPI endpoint:" -ForegroundColor Cyan
Write-Host "  http://localhost:8080/v3/api-docs" -ForegroundColor Blue
Write-Host ""
Write-Host "Press Ctrl+C to stop the backend when done." -ForegroundColor Gray
Write-Host ""

& .\gradlew.bat bootRun

