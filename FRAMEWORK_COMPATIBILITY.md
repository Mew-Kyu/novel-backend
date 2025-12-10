# 🎨 Framework Compatibility Guide

## ✅ TL;DR: Generated Code Hoạt Động Với Framework Nào?

**Câu trả lời: TẤT CẢ!**

Generated code từ `generate-frontend.ps1` tạo ra:
- ✅ **TypeScript interfaces** (universal)
- ✅ **Axios API client** (framework-agnostic)
- ✅ **React hooks** (useApi, useMutation)

→ Bạn có thể dùng với **BẤT KỲ framework TypeScript/JavaScript** nào!

---

## 📦 Generated Code Structure

```
generated-api/
├── types/
│   └── models.ts          # ✅ Universal TypeScript types
├── api/
│   ├── client.ts          # ✅ Axios client (works everywhere)
│   └── hooks.ts           # ⚛️ React-specific hooks
├── package.json
└── README.md
```

---

## 🎯 Framework Support Matrix

| Framework | API Client | TypeScript Types | React Hooks | Notes |
|-----------|-----------|-----------------|-------------|-------|
| **React** | ✅ | ✅ | ✅ | Full support out of the box |
| **Next.js** | ✅ | ✅ | ✅ | Works with App Router & Pages Router |
| **Vue 3** | ✅ | ✅ | ➖ | Use Composables (examples provided) |
| **Angular** | ✅ | ✅ | ➖ | Wrap in Services (examples provided) |
| **Svelte** | ✅ | ✅ | ➖ | Use stores (works great) |
| **Vanilla JS/TS** | ✅ | ✅ | ➖ | Direct axios usage |
| **Any other** | ✅ | ✅ | ➖ | Just import apiClient |

✅ = Fully supported
➖ = Not applicable (framework uses different pattern)

---

## 🚀 Quick Start by Framework

### ⚛️ React / Next.js (Create React App, Vite)
```bash
npm install axios
```

**Dùng ngay hooks có sẵn:**
```typescript
import { useApi } from './api/hooks';
import { apiClient } from './api/client';

const { data, loading, error } = useApi(
  () => apiClient.get('/api/stories'),
  { immediate: true }
);
```

**Recommended: Thêm React Query**
```bash
npm install @tanstack/react-query
```

---

### 🔷 Next.js 13+ (App Router)
```bash
npm install axios
```

**Server Component (recommended):**
```tsx
async function getStories() {
  const res = await fetch('http://localhost:8080/api/stories');
  return res.json();
}

export default async function Page() {
  const stories = await getStories();
  return <div>{JSON.stringify(stories)}</div>;
}
```

**Client Component:**
```tsx
'use client';
import { apiClient } from './api/client';
// Use like React
```

---

### 💚 Vue 3
```bash
npm install axios
```

**Tạo composable của riêng bạn:**
```typescript
// composables/useApi.ts
import { ref, onMounted } from 'vue';
import { apiClient } from './api/client';

export function useApi(apiFunc, immediate = true) {
  const data = ref(null);
  const loading = ref(false);
  const execute = async () => {
    loading.value = true;
    data.value = await apiFunc();
    loading.value = false;
  };
  if (immediate) onMounted(execute);
  return { data, loading, execute };
}
```

**Hoặc dùng trực tiếp:**
```vue
<script setup>
import { ref, onMounted } from 'vue';
import { apiClient } from './api/client';

const stories = ref([]);
onMounted(async () => {
  stories.value = await apiClient.get('/api/stories');
});
</script>
```

---

### 🅰️ Angular
```bash
npm install axios
```

**Tạo service:**
```typescript
import { Injectable } from '@angular/core';
import { from } from 'rxjs';
import { apiClient } from './api/client';

@Injectable({ providedIn: 'root' })
export class ApiService {
  getStories() {
    return from(apiClient.get('/api/stories'));
  }
}
```

---

### 🔶 Svelte
```bash
npm install axios
```

**Store:**
```typescript
// stores/api.ts
import { writable } from 'svelte/store';
import { apiClient } from './api/client';

export const stories = writable([]);

export async function loadStories() {
  const data = await apiClient.get('/api/stories');
  stories.set(data);
}
```

**Component:**
```svelte
<script>
import { onMount } from 'svelte';
import { apiClient } from './api/client';

let stories = [];
let loading = true;

onMount(async () => {
  stories = await apiClient.get('/api/stories');
  loading = false;
});
</script>

{#if loading}
  Loading...
{:else}
  {#each stories as story}
    <div>{story.title}</div>
  {/each}
{/if}
```

---

## 💡 Core Concept: Framework-Agnostic API Client

**Điểm mạnh:** Generated `api/client.ts` là **framework-agnostic** (không phụ thuộc framework).

```typescript
// api/client.ts
import axios from 'axios';

class ApiClient {
  // Pure axios wrapper
  // No React, Vue, Angular dependencies
  async get(url) { return this.client.get(url); }
  async post(url, data) { return this.client.post(url, data); }
  // ...
}

export const apiClient = new ApiClient();
```

→ Bạn **import và dùng trong BẤT KỲ framework nào**!

---

## 🎯 What About React Hooks?

Generated `api/hooks.ts` chỉ dùng được trong React:

```typescript
// api/hooks.ts
import { useState, useEffect } from 'react'; // ← React specific
```

**Nếu dùng framework khác (Vue, Angular, Svelte):**
1. ✅ **Dùng `api/client.ts`** (universal)
2. ✅ **Dùng `types/models.ts`** (universal)
3. ❌ **Bỏ qua `api/hooks.ts`** (React-only)
4. ✅ **Tự tạo wrapper** theo pattern của framework (xem examples)

---

## 📝 Real-World Examples

### React Example
```typescript
import { useApi } from './api/hooks';
import { apiClient } from './api/client';
import { StoryDto } from './types/models';

function StoriesList() {
  const { data, loading } = useApi<StoryDto[]>(
    () => apiClient.get('/api/stories'),
    { immediate: true }
  );
  return <div>{data?.map(s => s.title)}</div>;
}
```

### Vue Example
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { apiClient } from './api/client';
import type { StoryDto } from './types/models';

const stories = ref<StoryDto[]>([]);

onMounted(async () => {
  stories.value = await apiClient.get('/api/stories');
});
</script>
```

### Angular Example
```typescript
import { Component, OnInit } from '@angular/core';
import { apiClient } from './api/client';
import { StoryDto } from './types/models';

@Component({...})
export class StoriesComponent implements OnInit {
  stories: StoryDto[] = [];

  async ngOnInit() {
    this.stories = await apiClient.get('/api/stories');
  }
}
```

### Vanilla TS Example
```typescript
import { apiClient } from './api/client';
import { StoryDto } from './types/models';

async function loadStories() {
  const stories: StoryDto[] = await apiClient.get('/api/stories');
  console.log(stories);
}
```

---

## 🔥 Advanced: Create Your Own Hooks/Composables

### Vue Composable (giống React hooks)
```typescript
// composables/useApi.ts
import { ref, Ref } from 'vue';
import { apiClient } from '../api/client';

export function useApi<T>(
  apiFunc: () => Promise<T>,
  immediate = true
): {
  data: Ref<T | null>;
  loading: Ref<boolean>;
  error: Ref<any>;
  execute: () => Promise<void>;
} {
  const data = ref<T | null>(null);
  const loading = ref(false);
  const error = ref<any>(null);

  const execute = async () => {
    loading.value = true;
    error.value = null;
    try {
      data.value = await apiFunc();
    } catch (err) {
      error.value = err;
    } finally {
      loading.value = false;
    }
  };

  if (immediate) {
    execute();
  }

  return { data, loading, error, execute };
}
```

### Svelte Store (giống React hooks)
```typescript
// stores/useApi.ts
import { writable } from 'svelte/store';

export function useApi<T>(apiFunc: () => Promise<T>) {
  const data = writable<T | null>(null);
  const loading = writable(true);
  const error = writable<any>(null);

  const execute = async () => {
    loading.set(true);
    try {
      const result = await apiFunc();
      data.set(result);
    } catch (err) {
      error.set(err);
    } finally {
      loading.set(false);
    }
  };

  execute();

  return { data, loading, error };
}
```

---

## 🎓 Best Practices

### 1. **Type Safety Everywhere**
```typescript
// ✅ GOOD: Use generated types
import { StoryDto, CreateStoryRequest } from './types/models';

async function createStory(data: CreateStoryRequest): Promise<StoryDto> {
  return apiClient.post('/api/stories', data);
}

// ❌ BAD: No types
async function createStory(data: any): Promise<any> {
  return apiClient.post('/api/stories', data);
}
```

### 2. **Reusable API Functions**
```typescript
// api/stories.ts
import { apiClient } from './api/client';
import { StoryDto, CreateStoryRequest } from './types/models';

export const storiesApi = {
  getAll: (): Promise<StoryDto[]> => 
    apiClient.get('/api/stories'),
    
  getById: (id: number): Promise<StoryDto> => 
    apiClient.get(`/api/stories/${id}`),
    
  create: (data: CreateStoryRequest): Promise<StoryDto> => 
    apiClient.post('/api/stories', data),
};
```

### 3. **Environment Variables**
```typescript
// api/client.ts
const BASE_URL = 
  process.env.REACT_APP_API_URL || // React
  process.env.NEXT_PUBLIC_API_URL || // Next.js
  import.meta.env.VITE_API_URL || // Vite
  'http://localhost:8080'; // Fallback
```

---

## ✨ Summary

### Core Generated Files:
1. **`types/models.ts`** → ✅ Works EVERYWHERE
2. **`api/client.ts`** → ✅ Works EVERYWHERE
3. **`api/hooks.ts`** → ⚛️ React ONLY

### Framework Usage:
- **React/Next.js** → Use everything as-is ✅
- **Vue/Angular/Svelte** → Use client + types, create own wrappers ✅
- **Vanilla TS** → Use client + types directly ✅

### One Line Answer:
> Generated code works with **ALL TypeScript/JavaScript frameworks**. 
> React hooks included, but you can easily create similar wrappers for other frameworks.

---

**📚 See Full Examples:** [QUICK_START_VIETNAM.md](QUICK_START_VIETNAM.md)

**🚀 Generate Code Now:**
```powershell
.\generate-frontend.ps1 -OutputPath "./my-frontend/src/api"
```

