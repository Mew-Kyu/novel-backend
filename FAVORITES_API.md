# Favorites Feature - API Documentation

## Overview
The Favorites feature allows users to bookmark their favorite stories. Users can add/remove stories from their favorites list and check the popularity of stories by viewing favorite counts.

## Database Schema
Migration file: `V9__add_favorites_table.sql`

Table: `favorites`
- `id` (BIGSERIAL): Primary key
- `user_id` (BIGINT): Foreign key to users table
- `story_id` (BIGINT): Foreign key to stories table
- `created_at` (TIMESTAMP): When the favorite was added

Constraints:
- Unique constraint on (user_id, story_id) - prevents duplicate favorites
- Cascade delete when user or story is deleted
- Indexes on user_id, story_id, and (user_id, created_at) for performance

## API Endpoints

### 1. Get User's Favorites
**Endpoint:** `GET /api/favorites`  
**Authentication:** Required (USER role)  
**Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort by (default: createdAt,desc)

**Response:** Paginated list of favorite stories with full story details

**Example:**
```bash
GET /api/favorites?page=0&size=20
```

---

### 2. Add Story to Favorites
**Endpoint:** `POST /api/favorites/{storyId}`  
**Authentication:** Required (USER role)  
**Path Variable:**
- `storyId`: ID of the story to add to favorites

**Response:** The created favorite object

**Example:**
```bash
POST /api/favorites/1
```

**Errors:**
- 404: Story not found
- 400: Story already in favorites

---

### 3. Remove Story from Favorites
**Endpoint:** `DELETE /api/favorites/{storyId}`  
**Authentication:** Required (USER role)  
**Path Variable:**
- `storyId`: ID of the story to remove from favorites

**Response:** 204 No Content

**Example:**
```bash
DELETE /api/favorites/1
```

**Errors:**
- 404: Story not in favorites

---

### 4. Check Favorite Status
**Endpoint:** `GET /api/favorites/check/{storyId}`  
**Authentication:** Required (USER role)  
**Path Variable:**
- `storyId`: ID of the story to check

**Response:**
```json
{
  "isFavorite": true,
  "favoriteCount": 42
}
```

**Example:**
```bash
GET /api/favorites/check/1
```

---

### 5. Get Favorite Count (Public)
**Endpoint:** `GET /api/favorites/count/{storyId}`  
**Authentication:** Not required  
**Path Variable:**
- `storyId`: ID of the story

**Response:** Number of users who favorited the story

**Example:**
```bash
GET /api/favorites/count/1
```

---

## Implementation Details

### Files Created:
1. **Database Migration:**
   - `V9__add_favorites_table.sql`

2. **Domain Layer:**
   - `Favorite.java` - Entity
   - `FavoriteRepository.java` - JPA Repository
   - `FavoriteDto.java` - Data Transfer Object
   - `FavoriteStatusDto.java` - Status DTO
   - `FavoriteService.java` - Business Logic

3. **Mapper:**
   - `FavoriteMapper.java` - MapStruct mapper

4. **Controller:**
   - `FavoriteController.java` - REST endpoints

5. **Postman Collection:**
   - Added "Favorites" section with all 5 endpoints

---

## Testing with Postman

1. Import the updated `Novel-Backend-API.postman_collection.json`
2. Login to get JWT token (auto-saved to collection variables)
3. Navigate to "Favorites" folder
4. Test the following workflow:
   - Get My Favorites (should be empty initially)
   - Add to Favorites (story ID = 1)
   - Get My Favorites (should show 1 item)
   - Check Favorite Status (should return isFavorite=true)
   - Get Favorite Count (public endpoint, no auth)
   - Remove from Favorites
   - Get My Favorites (should be empty again)

---

## Business Rules

1. **Unique Favorites:** A user can only favorite a story once
2. **Authentication:** Most endpoints require authentication except:
   - Get Favorite Count (public)
3. **Authorization:** Only the owner can manage their favorites
4. **Cascade Delete:** When a user or story is deleted, related favorites are automatically removed
5. **Sorting:** Favorites are sorted by creation date (newest first) by default

---

## Database Migration

To apply the migration:

```bash
# The migration will run automatically on application startup
# Or manually run:
./gradlew flywayMigrate
```

To verify the migration:
```sql
-- Check if table exists
SELECT * FROM favorites;

-- Check constraints
SELECT * FROM information_schema.table_constraints 
WHERE table_name = 'favorites';
```

---

## Integration with Frontend

Frontend can use these endpoints to:
1. Display a heart/bookmark icon (toggle based on `isFavorite` status)
2. Show favorite count on story cards
3. Create a "My Favorites" page
4. Add "Add to Favorites" button on story detail page
5. Show popular stories by favorite count

---

## Performance Considerations

- Indexes on `user_id` and `story_id` ensure fast queries
- Pagination prevents loading too many favorites at once
- Composite index on `(user_id, created_at)` optimizes sorting
- `existsByUserIdAndStoryId` uses database-level check for efficiency

---

## Future Enhancements

Possible improvements:
1. Add favorite categories/collections
2. Make favorites public/private
3. Add favorite notes/comments
4. Sort favorites by custom order
5. Export favorites list
6. Notification when favorited story updates

---

## Error Handling

The API returns standard HTTP status codes:
- `200 OK`: Successful GET request
- `201 Created`: Successfully added to favorites
- `204 No Content`: Successfully removed from favorites
- `400 Bad Request`: Already favorited or validation error
- `401 Unauthorized`: Not authenticated
- `404 Not Found`: Story not found or not in favorites
- `500 Internal Server Error`: Server error

