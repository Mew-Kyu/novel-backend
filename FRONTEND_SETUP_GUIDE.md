# 🚀 Frontend Development Quick Start Guide

## Giải pháp tự động generate code cho Frontend

Project này cung cấp **3 cách** để generate TypeScript code tự động từ backend, giúp bạn code frontend với **ít phải chỉnh tay nhất**.

---

## 📋 Tóm tắt các phương pháp

| Phương pháp | Ưu điểm | Nhược điểm | Khuyên dùng |
|-------------|---------|------------|-------------|
| **1. OpenAPI/Swagger** | ✅ Type-safe 100%<br>✅ Auto-complete tốt nhất<br>✅ Chuẩn công nghiệp | ⚠️ Cần backend chạy<br>⚠️ Cần cài tool | ⭐⭐⭐⭐⭐ |
| **2. Postman Collection** | ✅ Dễ dùng<br>✅ Không cần backend chạy<br>✅ Có sẵn collection | ⚠️ Type ít chi tiết hơn | ⭐⭐⭐⭐ |
| **3. Manual với Hooks** | ✅ Linh hoạt nhất<br>✅ Custom được | ⚠️ Phải viết thủ công | ⭐⭐⭐ |

---

## 🥇 Phương pháp 1: OpenAPI/Swagger (KHUYÊN DÙNG)

### Bước 1: Start backend
```bash
.\gradlew.bat bootRun
```

### Bước 2: Xem API Documentation
Mở browser: **http://localhost:8080/swagger-ui.html**

Swagger UI cung cấp:
- 📖 Tài liệu interactive của TẤT CẢ endpoints
- 🧪 Test API trực tiếp trên browser
- 📥 Download OpenAPI spec (JSON/YAML)

### Bước 3: Generate TypeScript Client
```powershell
# Generate vào folder frontend của bạn
.\generate-from-openapi.ps1 -OutputPath "../novel-frontend/src/api"

# Hoặc generate vào folder tạm
.\generate-from-openapi.ps1
```

### Bước 4: Sử dụng trong Frontend

```typescript
// src/api/index.ts
import apiClient from './api-client';

// Login
const login = async (username: string, password: string) => {
  const response = await apiClient.raw.apiAuthLoginPost({
    username,
    password
  });
  apiClient.setToken(response.data.accessToken);
  return response.data;
};

// Get stories (với type-safe)
const getStories = async () => {
  const response = await apiClient.raw.apiStoriesGet();
  return response.data; // TypeScript biết chính xác type này!
};

// Get story by ID
const getStory = async (id: number) => {
  const response = await apiClient.raw.apiStoriesIdGet(id);
  return response.data;
};
```

**React Component Example:**
```tsx
import { useState, useEffect } from 'react';
import apiClient, { StoryDto } from './api';

function StoriesList() {
  const [stories, setStories] = useState<StoryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient.raw.apiStoriesGet()
      .then(res => setStories(res.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      {loading ? 'Loading...' : stories.map(story => (
        <div key={story.id}>{story.title}</div>
      ))}
    </div>
  );
}
```

---

## 🥈 Phương pháp 2: Từ Postman Collection

### Bước 1: Generate từ Postman
```powershell
.\generate-frontend-code.ps1 -OutputPath "../novel-frontend/src/api"
```

Script này sẽ:
- ✅ Parse Postman collection có sẵn
- ✅ Generate API client functions
- ✅ Generate TypeScript types từ Java DTOs
- ✅ Generate React hooks

### Bước 2: Sử dụng
```typescript
import { apiClient } from './api/client';
import { useApi } from './api/hooks';

// Direct API call
const stories = await apiClient.get('/api/stories');

// With React hook
function MyComponent() {
  const { data, loading, error } = useApi(
    () => apiClient.get('/api/stories'),
    { immediate: true }
  );
  
  if (loading) return <div>Loading...</div>;
  return <div>{JSON.stringify(data)}</div>;
}
```

---

## 🥉 Phương pháp 3: Manual Setup với Best Practices

Nếu muốn control hoàn toàn, setup như sau:

### 1. Tạo API Client Base
```typescript
// src/api/client.ts
import axios from 'axios';

const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

// Auto attach token
client.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto handle 401
client.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default client;
```

### 2. Tạo API Functions
```typescript
// src/api/stories.ts
import client from './client';

export const storiesApi = {
  getAll: () => client.get('/api/stories'),
  getById: (id: number) => client.get(`/api/stories/${id}`),
  create: (data: any) => client.post('/api/stories', data),
  update: (id: number, data: any) => client.put(`/api/stories/${id}`, data),
  delete: (id: number) => client.delete(`/api/stories/${id}`),
};
```

### 3. Tạo React Query Hooks (Optional)
```typescript
// src/hooks/useStories.ts
import { useQuery, useMutation } from '@tanstack/react-query';
import { storiesApi } from '../api/stories';

export const useStories = () => {
  return useQuery({
    queryKey: ['stories'],
    queryFn: storiesApi.getAll
  });
};

export const useCreateStory = () => {
  return useMutation({
    mutationFn: storiesApi.create
  });
};
```

---

## 🎯 Khuyến nghị workflow

### Setup lần đầu (chọn 1 trong 2):

#### Option A: Dùng OpenAPI (best)
```powershell
# 1. Start backend
.\gradlew.bat bootRun

# 2. Generate client
.\generate-from-openapi.ps1 -OutputPath "../novel-frontend/src/api"

# 3. Install dependencies trong frontend
cd ../novel-frontend
npm install axios
```

#### Option B: Dùng Postman
```powershell
# Generate từ Postman collection
.\generate-frontend-code.ps1 -OutputPath "../novel-frontend/src/api"

cd ../novel-frontend
npm install axios
```

### Khi backend thay đổi:
```powershell
# Re-generate client (mất 5 giây)
.\generate-from-openapi.ps1 -OutputPath "../novel-frontend/src/api"
```

---

## 📚 Các API có sẵn

Tham khảo file **Novel-Backend-API.postman_collection.json** hoặc xem Swagger UI để biết full list endpoints:

### 🔐 Authentication
- POST `/api/auth/register` - Đăng ký
- POST `/api/auth/login` - Đăng nhập
- POST `/api/auth/refresh` - Refresh token

### 📚 Stories
- GET `/api/stories` - List stories (public)
- GET `/api/stories/{id}` - Get story detail (public)
- POST `/api/stories` - Create story (auth required)
- PUT `/api/stories/{id}` - Update story (auth required)
- DELETE `/api/stories/{id}` - Delete story (auth required)

### 📖 Chapters
- GET `/api/stories/{storyId}/chapters` - List chapters (public)
- GET `/api/stories/{storyId}/chapters/{chapterId}` - Get chapter (public)
- POST `/api/stories/{storyId}/chapters` - Create chapter (auth)
- PUT `/api/chapters/{id}` - Update chapter (auth)
- DELETE `/api/chapters/{id}` - Delete chapter (auth)

### ⭐ Favorites
- GET `/api/favorites` - User's favorites (auth)
- POST `/api/favorites/story/{storyId}` - Add favorite (auth)
- DELETE `/api/favorites/story/{storyId}` - Remove favorite (auth)

### 💬 Comments & Ratings
- GET `/api/comments/story/{storyId}` - Get comments (public)
- POST `/api/comments/story/{storyId}` - Add comment (auth)
- GET `/api/ratings/story/{storyId}/average` - Get avg rating (public)
- POST `/api/ratings/story/{storyId}` - Rate story (auth)

### 🤖 AI Features
- POST `/api/ai/search/semantic` - Semantic search (public)
- GET `/api/ai/recommendations/{userId}` - Get recommendations (auth)

### 📊 Stats & History
- GET `/api/reading-history` - Reading history (auth)
- POST `/api/reading-history` - Update history (auth)
- GET `/api/stats/dashboard` - User stats (auth)

### 👑 Admin
- GET `/api/admin/users` - List users (admin only)
- PUT `/api/admin/users/{id}/role` - Change role (admin only)

---

## 🔑 Authentication Flow

```typescript
// 1. Login
const response = await apiClient.raw.apiAuthLoginPost({
  username: 'user@example.com',
  password: 'password123'
});

// 2. Save token
apiClient.setToken(response.data.accessToken);
localStorage.setItem('refreshToken', response.data.refreshToken);

// 3. Token tự động được attach vào mọi request sau đó

// 4. Refresh token khi expired
const refreshResponse = await apiClient.raw.apiAuthRefreshPost({
  refreshToken: localStorage.getItem('refreshToken')
});
apiClient.setToken(refreshResponse.data.accessToken);

// 5. Logout
apiClient.clearToken();
localStorage.removeItem('refreshToken');
```

---

## 🎨 Frontend Framework Specific

### React
```bash
npm install axios @tanstack/react-query
```

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import apiClient from './api';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <YourApp />
    </QueryClientProvider>
  );
}
```

### Vue
```bash
npm install axios pinia
```

```typescript
// stores/api.ts
import { defineStore } from 'pinia';
import apiClient from './api';

export const useApiStore = defineStore('api', {
  state: () => ({ token: null }),
  actions: {
    setToken(token: string) {
      this.token = token;
      apiClient.setToken(token);
    }
  }
});
```

### Angular
```bash
npm install axios
```

```typescript
// services/api.service.ts
import { Injectable } from '@angular/core';
import apiClient from './api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  stories() {
    return apiClient.raw.apiStoriesGet();
  }
}
```

---

## 🐛 Troubleshooting

### Backend không chạy
```powershell
# Check health
curl http://localhost:8080/actuator/health

# Start backend
.\gradlew.bat bootRun
```

### CORS errors
Backend đã config CORS, nhưng nếu vẫn bị lỗi, check `CorsConfig.java`

### Token expired
```typescript
// Auto refresh token
axios.interceptors.response.use(
  res => res,
  async err => {
    if (err.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      const newToken = await refreshAccessToken(refreshToken);
      apiClient.setToken(newToken);
      // Retry request
      return axios(err.config);
    }
    return Promise.reject(err);
  }
);
```

---

## 📖 Tài liệu bổ sung

- **FAVORITES_API.md** - Chi tiết về Favorites API
- **IMPLEMENTATION_SUMMARY.md** - Tổng quan implementation
- **Novel-Backend-API.postman_collection.json** - Postman collection để test

---

## ✨ Tips & Best Practices

1. **Dùng TypeScript** - Tận dụng type safety
2. **Dùng React Query/SWR** - Quản lý cache và loading states
3. **Tách API logic** - Không gọi API trực tiếp trong components
4. **Error handling** - Luôn handle errors properly
5. **Loading states** - Show loading indicators
6. **Optimistic updates** - Update UI trước, sync sau
7. **Retry logic** - Auto retry failed requests
8. **Request cancellation** - Cancel requests khi component unmount

---

## 🚀 Quick Start Checklist

- [ ] Start backend: `.\gradlew.bat bootRun`
- [ ] Open Swagger UI: http://localhost:8080/swagger-ui.html
- [ ] Generate client: `.\generate-from-openapi.ps1`
- [ ] Copy generated code to frontend project
- [ ] Install axios: `npm install axios`
- [ ] Import and use: `import apiClient from './api'`
- [ ] Code frontend with full type safety! 🎉

---

**Happy Coding! 🚀**

