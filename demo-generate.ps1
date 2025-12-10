# Demo script - Generate và preview frontend code
# Sử dụng: .\demo-generate.ps1

Write-Host @"
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║   🚀 NOVEL BACKEND - FRONTEND CODE GENERATOR DEMO               ║
║                                                                  ║
║   Tự động generate TypeScript code từ backend Java              ║
║   → Ít phải chỉnh tay nhất khi code frontend!                   ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

Write-Host "`n📋 Bạn có 3 options để generate code:" -ForegroundColor Yellow
Write-Host "`n  [1] 🥇 OpenAPI/Swagger (KHUYÊN DÙNG - Type-safe 100%)" -ForegroundColor Green
Write-Host "      → Tự động generate từ Swagger spec" -ForegroundColor Gray
Write-Host "      → Full TypeScript types + API client" -ForegroundColor Gray
Write-Host "      → Cần backend đang chạy" -ForegroundColor Gray

Write-Host "`n  [2] 🥈 Postman Collection (Dễ dùng)" -ForegroundColor Green
Write-Host "      → Generate từ Postman collection có sẵn" -ForegroundColor Gray
Write-Host "      → Không cần backend chạy" -ForegroundColor Gray

Write-Host "`n  [3] 📖 Chỉ xem hướng dẫn" -ForegroundColor Green
Write-Host "      → Mở file FRONTEND_SETUP_GUIDE.md" -ForegroundColor Gray

Write-Host "`n  [4] 🌐 Mở Swagger UI (nếu backend đang chạy)" -ForegroundColor Green
Write-Host "      → Test API trực tiếp trên browser" -ForegroundColor Gray

Write-Host "`n  [0] Thoát" -ForegroundColor Red

$choice = Read-Host "`nChọn option (0-4)"

switch ($choice) {
    "1" {
        Write-Host "`n🔍 Checking if backend is running..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            Write-Host "✅ Backend is running!" -ForegroundColor Green

            Write-Host "`n🚀 Starting OpenAPI generation..." -ForegroundColor Cyan
            .\generate-from-openapi.ps1 -OutputPath "./frontend-generated-openapi"

            Write-Host "`n✨ Preview generated files:" -ForegroundColor Cyan
            Get-ChildItem -Path "./frontend-generated-openapi" -Recurse -File | Select-Object -First 10 | ForEach-Object {
                Write-Host "   📄 $($_.FullName)" -ForegroundColor Gray
            }

            Write-Host "`n✅ Done! Check folder: ./frontend-generated-openapi" -ForegroundColor Green
        } catch {
            Write-Host "❌ Backend is not running!" -ForegroundColor Red
            Write-Host "`nPlease start backend first:" -ForegroundColor Yellow
            Write-Host "   .\gradlew.bat bootRun" -ForegroundColor White
            Write-Host "`nOr use Option 2 (Postman Collection) instead" -ForegroundColor Gray
        }
    }

    "2" {
        Write-Host "`n🚀 Starting Postman-based generation..." -ForegroundColor Cyan
        .\generate-frontend-code.ps1 -OutputPath "./frontend-generated"

        Write-Host "`n✨ Preview generated files:" -ForegroundColor Cyan
        Get-ChildItem -Path "./frontend-generated" -Recurse -File | ForEach-Object {
            Write-Host "   📄 $($_.FullName)" -ForegroundColor Gray
        }

        Write-Host "`n✅ Done! Check folder: ./frontend-generated" -ForegroundColor Green
    }

    "3" {
        Write-Host "`n📖 Opening guide..." -ForegroundColor Cyan
        Start-Process "FRONTEND_SETUP_GUIDE.md"
        Write-Host "✅ Guide opened in your default editor!" -ForegroundColor Green
    }

    "4" {
        Write-Host "`n🔍 Checking backend..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            Write-Host "✅ Backend is running!" -ForegroundColor Green
            Write-Host "`n🌐 Opening Swagger UI..." -ForegroundColor Cyan
            Start-Process "http://localhost:8080/swagger-ui.html"
            Write-Host "✅ Swagger UI opened in browser!" -ForegroundColor Green
        } catch {
            Write-Host "❌ Backend is not running!" -ForegroundColor Red
            Write-Host "`nPlease start backend first:" -ForegroundColor Yellow
            Write-Host "   .\gradlew.bat bootRun" -ForegroundColor White
        }
    }

    "0" {
        Write-Host "`n👋 Bye!" -ForegroundColor Cyan
        exit
    }

    default {
        Write-Host "`n❌ Invalid choice!" -ForegroundColor Red
    }
}

Write-Host "`n📚 Quick Tips:" -ForegroundColor Cyan
Write-Host "  • Dùng option 1 (OpenAPI) để có type-safe tốt nhất" -ForegroundColor Gray
Write-Host "  • Khi backend APIs thay đổi, chỉ cần re-run script là xong" -ForegroundColor Gray
Write-Host "  • Copy folder generated vào frontend project của bạn" -ForegroundColor Gray
Write-Host "  • Đọc file README.md trong folder generated để biết cách dùng" -ForegroundColor Gray

Write-Host "`n📖 Full guide: FRONTEND_SETUP_GUIDE.md" -ForegroundColor Yellow
Write-Host "`nPress any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

