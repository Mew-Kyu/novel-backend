# Script to generate TypeScript code from OpenAPI/Swagger
# Requirements: npm install -g @openapitools/openapi-generator-cli
# Usage: .\generate-from-openapi-clean.ps1 -OutputPath "../novel-frontend/src/api"

param(
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = "./frontend-generated-openapi",
    [Parameter(Mandatory=$false)]
    [string]$ApiUrl = "http://localhost:8080/v3/api-docs"
)

Write-Host "[*] Generating TypeScript Client from OpenAPI..." -ForegroundColor Cyan

# Check if backend is running
Write-Host "`n[1/5] Checking if backend is running..." -ForegroundColor Yellow

$healthEndpoints = @(
    "http://localhost:8080/actuator/health",
    "http://localhost:8080/health",
    "http://localhost:8080/api/health"
)

$backendRunning = $false
foreach ($healthUrl in $healthEndpoints) {
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
        Write-Host "[OK] Backend is running! (checked via $healthUrl)" -ForegroundColor Green
        $backendRunning = $true
        break
    } catch {
        continue
    }
}

if (-not $backendRunning) {
    Write-Host "[WARNING] Cannot confirm backend is running" -ForegroundColor Yellow
    Write-Host "          Tried: $($healthEndpoints -join ', ')" -ForegroundColor Gray
    Write-Host "`n[!] If the backend is NOT running, start it with:" -ForegroundColor Yellow
    Write-Host "    .\gradlew.bat bootRun" -ForegroundColor White
    Write-Host "`n[!] Continuing anyway... (OpenAPI endpoint might still work)" -ForegroundColor Yellow
}

# Create output directory
New-Item -ItemType Directory -Force -Path $OutputPath | Out-Null

# Download OpenAPI spec
Write-Host "`n[2/5] Downloading OpenAPI specification..." -ForegroundColor Green

# Try multiple possible endpoints
$apiEndpoints = @(
    $ApiUrl,
    "http://localhost:8080/v3/api-docs",
    "http://localhost:8080/v3/api-docs.yaml",
    "http://localhost:8080/api-docs"
)

$specDownloaded = $false
$lastError = $null

foreach ($endpoint in $apiEndpoints) {
    Write-Host "    Trying: $endpoint" -ForegroundColor Gray
    try {
        $response = Invoke-WebRequest -Uri $endpoint -UseBasicParsing -TimeoutSec 10

        # Save the spec
        $response.Content | Out-File -FilePath "$OutputPath/openapi.json" -Encoding UTF8

        Write-Host "[OK] OpenAPI spec downloaded from: $endpoint" -ForegroundColor Green
        $specDownloaded = $true
        break
    } catch {
        $lastError = $_
        Write-Host "    Failed: $($_.Exception.Message)" -ForegroundColor DarkGray
        continue
    }
}

if (-not $specDownloaded) {
    Write-Host "`n[ERROR] Failed to download OpenAPI spec from any endpoint" -ForegroundColor Red
    Write-Host "`nTried endpoints:" -ForegroundColor Yellow
    foreach ($endpoint in $apiEndpoints) {
        Write-Host "  - $endpoint" -ForegroundColor Gray
    }
    Write-Host "`nPossible causes:" -ForegroundColor Yellow
    Write-Host "  1. Backend is not running" -ForegroundColor Gray
    Write-Host "     → Start with: .\gradlew.bat bootRun" -ForegroundColor DarkGray
    Write-Host "  2. Swagger/OpenAPI is not properly configured" -ForegroundColor Gray
    Write-Host "     → Check that springdoc-openapi dependency is in build.gradle" -ForegroundColor DarkGray
    Write-Host "  3. Security is blocking the endpoint" -ForegroundColor Gray
    Write-Host "     → Verify SecurityConfig permits /v3/api-docs/**" -ForegroundColor DarkGray
    Write-Host "  4. Wrong port (using 8080)" -ForegroundColor Gray
    Write-Host "     → Check server.port in application.yml" -ForegroundColor DarkGray
    Write-Host "`nLast error: $($lastError.Exception.Message)" -ForegroundColor DarkRed

    # Try to give more specific help
    Write-Host "`nDebug steps:" -ForegroundColor Cyan
    Write-Host "  1. Check if backend is running:" -ForegroundColor White
    Write-Host "     curl http://localhost:8080/actuator/health" -ForegroundColor Gray
    Write-Host "  2. Open Swagger UI in browser:" -ForegroundColor White
    Write-Host "     http://localhost:8080/swagger-ui/index.html" -ForegroundColor Gray
    Write-Host "  3. View raw OpenAPI spec:" -ForegroundColor White
    Write-Host "     http://localhost:8080/v3/api-docs" -ForegroundColor Gray

    exit 1
}

# Check if openapi-generator-cli is installed
Write-Host "`n[3/5] Checking for openapi-generator-cli..." -ForegroundColor Yellow
$generatorInstalled = Get-Command openapi-generator-cli -ErrorAction SilentlyContinue

if (-not $generatorInstalled) {
    Write-Host "[!] openapi-generator-cli not found. Installing..." -ForegroundColor Yellow
    Write-Host "    Running: npm install -g @openapitools/openapi-generator-cli" -ForegroundColor Gray
    npm install -g @openapitools/openapi-generator-cli
}

# Generate TypeScript axios client
Write-Host "`n[4/5] Generating TypeScript client..." -ForegroundColor Green
openapi-generator-cli generate -i "$OutputPath/openapi.json" -g typescript-axios -o "$OutputPath/generated" --additional-properties=supportsES6=true,withSeparateModelsAndApi=true,apiPackage=api,modelPackage=models

Write-Host "[OK] TypeScript client generated!" -ForegroundColor Green

# Create custom wrapper for easier usage
Write-Host "`n[5/5] Creating custom API wrapper..." -ForegroundColor Green

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
    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', token);
    }
  }

  clearToken() {
    this.token = null;
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
    }
  }

  getToken(): string | null {
    if (!this.token && typeof window !== 'undefined') {
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
Write-Host "[OK] Custom wrapper created!" -ForegroundColor Green

# Create README
$readmeContent = @"
# Auto-Generated TypeScript Client from OpenAPI

Generated at: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Installation

1. Copy this folder to your frontend project
2. Install dependencies:

``````bash
npm install axios
``````

## Usage

### Basic Usage

``````typescript
import apiClient from '@/api';

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
import apiClient from '@/api';

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

## Regenerate

When backend APIs change, regenerate the client:

``````bash
.\generate-from-openapi-clean.ps1 -OutputPath "./src/api"
``````

## Available APIs

All API endpoints are available through apiClient.raw.*

Check the OpenAPI documentation at: http://localhost:8080/swagger-ui.html

## Authentication

``````typescript
// After login
apiClient.setToken(accessToken);

// Logout
apiClient.clearToken();

// Check current token
const token = apiClient.getToken();
``````

## Type Safety

All request/response types are automatically generated and available:

``````typescript
import { StoryDto, ChapterDto, LoginRequest } from '@/api';

const story: StoryDto = await apiClient.raw.apiStoriesIdGet(1);
``````
"@

Set-Content -Path "$OutputPath/README.md" -Value $readmeContent -Encoding UTF8

Write-Host "`n[SUCCESS] Complete!" -ForegroundColor Cyan
Write-Host "Output: $OutputPath" -ForegroundColor Yellow
Write-Host "`nView API Documentation:" -ForegroundColor Cyan
Write-Host "  http://localhost:8080/swagger-ui.html" -ForegroundColor Blue
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "  1. Install axios: npm install axios" -ForegroundColor Gray
Write-Host "  2. Import and use the generated API client" -ForegroundColor Gray
Write-Host "  3. Check README.md in the output folder for examples" -ForegroundColor Gray
