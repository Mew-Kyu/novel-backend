# Script để generate TypeScript code từ OpenAPI/Swagger
# Cần cài đặt: npm install -g @openapitools/openapi-generator-cli
# Sử dụng: .\generate-from-openapi.ps1 -OutputPath "../novel-frontend/src/api"

param(
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = "./frontend-generated-openapi",
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "http://localhost:8080/v3/api-docs"
)

Write-Host "🚀 Generating TypeScript Client from OpenAPI..." -ForegroundColor Cyan

# Kiểm tra xem backend có đang chạy không
Write-Host "`n🔍 Checking if backend is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Backend is running!" -ForegroundColor Green
} catch {
    Write-Host "❌ Backend is not running. Please start it first:" -ForegroundColor Red
    Write-Host "   .\gradlew.bat bootRun" -ForegroundColor Gray
    exit 1
}

# Tạo thư mục output
New-Item -ItemType Directory -Force -Path $OutputPath | Out-Null

# Download OpenAPI spec
Write-Host "`n📥 Downloading OpenAPI specification..." -ForegroundColor Green
try {
    Invoke-WebRequest -Uri $ApiUrl -OutFile "$OutputPath/openapi.json" -UseBasicParsing
    Write-Host "✅ OpenAPI spec downloaded" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to download OpenAPI spec from $ApiUrl" -ForegroundColor Red
    Write-Host "   Make sure backend is running and Swagger is enabled" -ForegroundColor Gray
    exit 1
}

# Kiểm tra xem openapi-generator-cli đã cài chưa
Write-Host "`n🔍 Checking for openapi-generator-cli..." -ForegroundColor Yellow
$generatorInstalled = Get-Command openapi-generator-cli -ErrorAction SilentlyContinue

if (-not $generatorInstalled) {
    Write-Host "⚠️  openapi-generator-cli not found. Installing..." -ForegroundColor Yellow
    Write-Host "   Running: npm install -g @openapitools/openapi-generator-cli" -ForegroundColor Gray
    npm install -g @openapitools/openapi-generator-cli
}

# Generate TypeScript axios client
Write-Host "`n⚙️  Generating TypeScript client..." -ForegroundColor Green
openapi-generator-cli generate `
    -i "$OutputPath/openapi.json" `
    -g typescript-axios `
    -o "$OutputPath/generated" `
    --additional-properties=supportsES6=true,withSeparateModelsAndApi=true,apiPackage=api,modelPackage=models

Write-Host "✅ TypeScript client generated!" -ForegroundColor Green

# Tạo custom wrapper để dễ sử dụng hơn
Write-Host "`n📝 Creating custom API wrapper..." -ForegroundColor Green

$wrapperContent = @"
// Custom API wrapper for easier usage
import { Configuration, DefaultApi } from './generated';

export class NovelApiClient {
  private api: DefaultApi;
  private token: string | null = null;

  constructor(basePath: string = 'http://localhost:8080') {
    const config = new Configuration({
      basePath,
      accessToken: () => this.token || '',
    });
    this.api = new DefaultApi(config);
  }

  // Authentication
  setToken(token: string) {
    this.token = token;
    localStorage.setItem('accessToken', token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('accessToken');
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('accessToken');
    }
    return this.token;
  }

  // Re-export API instance for direct access
  get raw() {
    return this.api;
  }
}

// Create singleton instance
const apiClient = new NovelApiClient();

// Auto-load token from localStorage
if (typeof window !== 'undefined') {
  const savedToken = localStorage.getItem('accessToken');
  if (savedToken) {
    apiClient.setToken(savedToken);
  }
}

export default apiClient;

// Re-export all types and models
export * from './generated';
"@

Set-Content -Path "$OutputPath/index.ts" -Value $wrapperContent -Encoding UTF8
Write-Host "✅ Custom wrapper created!" -ForegroundColor Green

# Tạo README
$readmeContent = @"
# Auto-Generated TypeScript Client from OpenAPI

Generated at: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## 📦 Installation

1. Copy this folder to your frontend project
2. Install dependencies:

``````bash
npm install axios
``````

## 🚀 Usage

### Basic Usage

``````typescript
import apiClient from './api-client';

// Login
const loginResponse = await apiClient.raw.apiAuthLoginPost({
  username: 'user@example.com',
  password: 'password123'
});

apiClient.setToken(loginResponse.data.accessToken);

// Get stories
const stories = await apiClient.raw.apiStoriesGet();

// Get specific story
const story = await apiClient.raw.apiStoriesIdGet(123);
``````

### With React

``````typescript
import { useState, useEffect } from 'react';
import apiClient from './api-client';

function StoriesList() {
  const [stories, setStories] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadStories() {
      try {
        const response = await apiClient.raw.apiStoriesGet();
        setStories(response.data);
      } catch (error) {
        console.error('Failed to load stories:', error);
      } finally {
        setLoading(false);
      }
    }
    loadStories();
  }, []);

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      {stories.map(story => (
        <div key={story.id}>{story.title}</div>
      ))}
    </div>
  );
}
``````

## 🔄 Regenerate

When backend APIs change, regenerate the client:

``````bash
.\generate-from-openapi.ps1 -OutputPath "./src/api"
``````

## 📚 Available APIs

All API endpoints are available through ``apiClient.raw.*``

Check the OpenAPI documentation at: http://localhost:8080/swagger-ui.html

## 🔑 Authentication

``````typescript
// After login
apiClient.setToken(accessToken);

// Logout
apiClient.clearToken();

// Check current token
const token = apiClient.getToken();
``````

## 📖 Type Safety

All request/response types are automatically generated and available:

``````typescript
import { StoryDto, ChapterDto, LoginRequest } from './api-client';

const story: StoryDto = await apiClient.raw.apiStoriesIdGet(1);
``````
"@

Set-Content -Path "$OutputPath/README.md" -Value $readmeContent -Encoding UTF8

Write-Host "`n✨ Complete!" -ForegroundColor Cyan
Write-Host "📂 Output: $OutputPath" -ForegroundColor Yellow
Write-Host "`n🌐 View API Documentation:" -ForegroundColor Cyan
Write-Host "   http://localhost:8080/swagger-ui.html" -ForegroundColor Blue
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "  1. Copy $OutputPath to your frontend project" -ForegroundColor Gray
Write-Host "  2. Run: npm install axios" -ForegroundColor Gray
Write-Host "  3. Import and use: import apiClient from './api-client'" -ForegroundColor Gray

