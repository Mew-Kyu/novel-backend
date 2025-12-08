# Novel Backend

## Setup

### Environment Variables

This project uses environment variables for sensitive configuration. Follow these steps:

1. Copy the `.env.example` file to `.env`:
   ```powershell
   Copy-Item .env.example .env
   ```

2. Edit `.env` and replace the placeholder values with your actual credentials:
   - `GEMINI_API_KEY`: Your Google Gemini API key
   - `JWT_SECRET`: A secure random string (at least 256 bits for HS256)

3. The `.env` file is already added to `.gitignore` and will not be committed to version control.

### Running the Application

```powershell
./gradlew bootRun
```

## Role-Based Access Control (RBAC)

The application implements role-based access control with three predefined roles:

### Roles

| Role | Description |
|------|-------------|
| **ADMIN** | Full access to all features including user management, role assignment, and system administration |
| **MODERATOR** | Can manage content (stories, chapters, genres) and moderate comments |
| **USER** | Basic user with ability to read content, rate, comment, and manage their own data |

### Endpoint Access Matrix

| Endpoint | Public | USER | MODERATOR | ADMIN |
|----------|--------|------|-----------|-------|
| **Authentication** |
| POST /api/auth/register | ✅ | ✅ | ✅ | ✅ |
| POST /api/auth/login | ✅ | ✅ | ✅ | ✅ |
| **Stories** |
| GET /api/stories | ✅ | ✅ | ✅ | ✅ |
| GET /api/stories/{id} | ✅ | ✅ | ✅ | ✅ |
| POST /api/stories | ❌ | ❌ | ✅ | ✅ |
| PUT /api/stories/{id} | ❌ | ❌ | ✅ | ✅ |
| DELETE /api/stories/{id} | ❌ | ❌ | ✅ | ✅ |
| **Chapters** |
| GET /api/stories/{id}/chapters | ✅ | ✅ | ✅ | ✅ |
| POST /api/stories/{id}/chapters | ❌ | ❌ | ✅ | ✅ |
| PUT /api/stories/{id}/chapters/{id} | ❌ | ❌ | ✅ | ✅ |
| DELETE /api/stories/{id}/chapters/{id} | ❌ | ❌ | ✅ | ✅ |
| **Genres** |
| GET /api/genres | ✅ | ✅ | ✅ | ✅ |
| POST /api/genres | ❌ | ❌ | ✅ | ✅ |
| PUT /api/genres/{id} | ❌ | ❌ | ✅ | ✅ |
| DELETE /api/genres/{id} | ❌ | ❌ | ✅ | ✅ |
| **Comments** |
| GET /api/comments/story/{id} | ✅ | ✅ | ✅ | ✅ |
| POST /api/comments | ❌ | ✅ | ✅ | ✅ |
| PUT /api/comments/{id} | ❌ | Owner | ✅ | ✅ |
| DELETE /api/comments/{id} | ❌ | Owner | ✅ | ✅ |
| **Ratings** |
| GET /api/ratings/story/{id} | ✅ | ✅ | ✅ | ✅ |
| POST /api/ratings | ❌ | ✅ | ✅ | ✅ |
| PUT /api/ratings/{id} | ❌ | Owner | Owner | Owner |
| DELETE /api/ratings/{id} | ❌ | Owner | Owner | Owner |
| **Favorites** |
| GET /api/favorites | ❌ | ✅ | ✅ | ✅ |
| POST /api/favorites/{storyId} | ❌ | ✅ | ✅ | ✅ |
| DELETE /api/favorites/{storyId} | ❌ | ✅ | ✅ | ✅ |
| GET /api/favorites/check/{storyId} | ❌ | ✅ | ✅ | ✅ |
| GET /api/favorites/count/{storyId} | ✅ | ✅ | ✅ | ✅ |
| **Crawl Jobs** |
| All /api/jobs/** | ❌ | ❌ | ✅ | ✅ |
| POST /api/crawl/syosetu | ❌ | ❌ | ✅ | ✅ |
| **AI Services** |
| POST /api/ai/search/semantic | ✅ | ✅ | ✅ | ✅ |
| POST /api/ai/translate/** | ❌ | ❌ | ✅ | ✅ |
| POST /api/ai/embeddings/** | ❌ | ❌ | ✅ | ✅ |
| **Admin** |
| All /api/admin/** | ❌ | ❌ | ❌ | ✅ |

### Admin API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/admin/users` | GET | List all users with roles (paginated) |
| `/api/admin/users/{userId}` | GET | Get user by ID |
| `/api/admin/users/{userId}/activate` | PATCH | Activate a user account |
| `/api/admin/users/{userId}/deactivate` | PATCH | Deactivate a user account |
| `/api/admin/users/{userId}/roles/{roleName}` | POST | Assign a role to a user |
| `/api/admin/users/{userId}/roles/{roleName}` | DELETE | Remove a role from a user |
| `/api/admin/roles` | GET | List all roles |
| `/api/admin/roles/{roleId}` | GET | Get role by ID |
| `/api/admin/roles` | POST | Create a new role |
| `/api/admin/roles/{roleId}` | PUT | Update a role |
| `/api/admin/roles/{roleId}` | DELETE | Delete a custom role |
| `/api/admin/stats/users` | GET | Get user statistics |

### User Registration with Role

When registering, you can optionally specify a role:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "displayName": "User Name",
  "roleName": "USER"
}
```

If `roleName` is not specified, the user will be assigned the `USER` role by default.

## Important Notes

- **Never commit the `.env` file** - it contains sensitive credentials
- Always use `.env.example` as a template for new developers
- Make sure to set environment variables in production deployments



