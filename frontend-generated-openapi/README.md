# Auto-Generated TypeScript Client from OpenAPI

Generated at: 2025-12-17 23:22:33

## Installation

1. Copy this folder to your frontend project
2. Install dependencies:

```bash
npm install axios
```

## Usage

### Basic Usage

```typescript
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
```

### With React

```typescript
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
```

## Regenerate

When backend APIs change, regenerate the client:

```bash
.\generate-from-openapi-clean.ps1 -OutputPath "./src/api"
```

## Available APIs

All API endpoints are available through apiClient.raw.*

Check the OpenAPI documentation at: http://localhost:8080/swagger-ui.html

## Authentication

```typescript
// After login
apiClient.setToken(accessToken);

// Logout
apiClient.clearToken();

// Check current token
const token = apiClient.getToken();
```

## Type Safety

All request/response types are automatically generated and available:

```typescript
import { StoryDto, ChapterDto, LoginRequest } from '@/api';

const story: StoryDto = await apiClient.raw.apiStoriesIdGet(1);
```
