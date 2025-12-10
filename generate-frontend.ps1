# Script tự động generate TypeScript types và API client từ backend
# Sử dụng: .\generate-frontend.ps1 -OutputPath "../novel-frontend/src/api"

param(
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = "./frontend-generated",
    [Parameter(Mandatory=$false)]
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Frontend Code Generator" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Output: $OutputPath" -ForegroundColor Yellow
Write-Host ""

# Tạo thư mục output
New-Item -ItemType Directory -Force -Path "$OutputPath\types" | Out-Null
New-Item -ItemType Directory -Force -Path "$OutputPath\api" | Out-Null

# Generate TypeScript types từ Java DTOs
Write-Host "[1/4] Generating TypeScript types from Java DTOs..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path ".\src\main\java" -Filter "*.java" -Recurse | Where-Object {
    $_.Name -match "(Dto|Request|Response)\.java$"
}

$allInterfaces = @()

foreach ($file in $javaFiles) {
    $content = Get-Content $file.FullName -Raw
    $className = $file.BaseName

    if ($content -notmatch "abstract class|interface |enum ") {
        Write-Host "  - Processing: $className" -ForegroundColor Gray

        $fieldLines = @()
        $pattern = 'private\s+(\w+(?:<[\w,\s<>]+>)?)\s+(\w+);'
        $matches = [regex]::Matches($content, $pattern)

        foreach ($match in $matches) {
            $javaType = $match.Groups[1].Value
            $fieldName = $match.Groups[2].Value

            $tsType = "any"
            if ($javaType -eq "String") { $tsType = "string" }
            elseif ($javaType -match "Integer|Long|Double|Float|int|long|double|float") { $tsType = "number" }
            elseif ($javaType -match "Boolean|boolean") { $tsType = "boolean" }
            elseif ($javaType -match "LocalDateTime|Date|Instant") { $tsType = "string" }
            elseif ($javaType -match "List<(.+)>") { $tsType = "$($Matches[1])[]" }
            elseif ($javaType -match "Set<(.+)>") { $tsType = "$($Matches[1])[]" }
            elseif ($javaType -match "Map<") { $tsType = "Record<string, any>" }

            $fieldLines += "  $fieldName`: $tsType;"
        }

        if ($fieldLines.Count -gt 0) {
            $allInterfaces += "export interface $className {"
            $allInterfaces += $fieldLines
            $allInterfaces += "}"
            $allInterfaces += ""
        }
    }
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$typesFileContent = "// Auto-generated TypeScript types from Java backend`r`n"
$typesFileContent += "// Generated at: $timestamp`r`n"
$typesFileContent += "// DO NOT EDIT MANUALLY`r`n`r`n"
$typesFileContent += $($allInterfaces -join "`r`n")

[System.IO.File]::WriteAllText("$OutputPath\types\models.ts", $typesFileContent)
Write-Host "  Generated: types\models.ts" -ForegroundColor Green

# Generate API client
Write-Host "[2/4] Generating API client..." -ForegroundColor Green

$apiClientContent = @"
// Auto-generated API client
// Generated at: $timestamp

import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

const BASE_URL = '$BaseUrl';

class ApiClient {
  private client: AxiosInstance;

  constructor(baseURL: string = BASE_URL) {
    this.client = axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor for auth
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = 'Bearer ' + token;
      }
      return config;
    });

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response.data,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('accessToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.get(url, config);
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.client.post(url, data, config);
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.client.put(url, data, config);
  }

  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.client.patch(url, data, config);
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.delete(url, config);
  }

  setToken(token: string) {
    this.client.defaults.headers.common['Authorization'] = 'Bearer ' + token;
    localStorage.setItem('accessToken', token);
  }

  clearToken() {
    delete this.client.defaults.headers.common['Authorization'];
    localStorage.removeItem('accessToken');
  }

  setBaseURL(baseURL: string) {
    this.client.defaults.baseURL = baseURL;
  }
}

const apiClient = new ApiClient();

export { apiClient };
export default apiClient;
"@

[System.IO.File]::WriteAllText("$OutputPath\api\client.ts", $apiClientContent)
Write-Host "  Generated: api\client.ts" -ForegroundColor Green

# Generate React hooks
Write-Host "[3/4] Generating React hooks..." -ForegroundColor Green

$hooksContent = @"
// Auto-generated React hooks for API calls
// Generated at: $timestamp

import { useState, useEffect, useCallback } from 'react';

interface UseApiOptions<T> {
  immediate?: boolean;
  onSuccess?: (data: T) => void;
  onError?: (error: any) => void;
}

export function useApi<T>(
  apiFunc: (...args: any[]) => Promise<T>,
  options: UseApiOptions<T> = {}
) {
  const { immediate = false, onSuccess, onError } = options;

  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<any>(null);

  const execute = useCallback(async (...args: any[]) => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiFunc(...args);
      setData(result);
      onSuccess?.(result);
      return result;
    } catch (err) {
      setError(err);
      onError?.(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [apiFunc, onSuccess, onError]);

  useEffect(() => {
    if (immediate) {
      execute();
    }
  }, [immediate, execute]);

  return { data, loading, error, execute, refetch: execute };
}

export function useMutation<T>(
  apiFunc: (...args: any[]) => Promise<T>
) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<any>(null);

  const mutate = useCallback(async (...args: any[]) => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiFunc(...args);
      return result;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [apiFunc]);

  return { mutate, loading, error };
}
"@

[System.IO.File]::WriteAllText("$OutputPath\api\hooks.ts", $hooksContent)
Write-Host "  Generated: api\hooks.ts" -ForegroundColor Green

# Generate README
Write-Host "[4/4] Generating README..." -ForegroundColor Green

$readmeContent = @"
# Auto-Generated Frontend API Client

Generated at: $timestamp

## Installation

``````bash
npm install axios
``````

## Usage

``````typescript
import { apiClient } from './api/client';

// Login
const response = await apiClient.post('/api/auth/login', {
  username: 'user@example.com',
  password: 'password123'
});

apiClient.setToken(response.accessToken);

// Get stories
const stories = await apiClient.get('/api/stories');

// Create story
const newStory = await apiClient.post('/api/stories', {
  title: 'My Story',
  description: 'A great story'
});
``````

## With React

``````typescript
import { useApi } from './api/hooks';
import { apiClient } from './api/client';

function StoriesList() {
  const { data, loading, error } = useApi(
    () => apiClient.get('/api/stories'),
    { immediate: true }
  );

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error!</div>;

  return <div>{JSON.stringify(data)}</div>;
}
``````

## Re-generate

``````bash
.\generate-frontend.ps1 -OutputPath "./src/api"
``````
"@

[System.IO.File]::WriteAllText("$OutputPath\README.md", $readmeContent)
Write-Host "  Generated: README.md" -ForegroundColor Green

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Generation Complete!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Output: $OutputPath" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Copy to your frontend project" -ForegroundColor White
Write-Host "  2. Run: npm install axios" -ForegroundColor White
Write-Host "  3. Import and use!" -ForegroundColor White

