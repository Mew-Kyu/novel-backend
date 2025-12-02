# Postman Collection - Hướng Dẫn Sử Dụng

## File Collection
- **File**: `Novel-Backend-API.postman_collection.json`
- **Version**: v2.1.0

## Cách Import vào Postman

1. Mở Postman
2. Click **Import** ở góc trên bên trái
3. Chọn file `Novel-Backend-API.postman_collection.json`
4. Collection sẽ được import với tên **"Novel Backend API"**

## Cấu Hình Variables

Collection có 2 biến:

- `base_url`: URL của server (mặc định: `http://localhost:8080`)
- `jwt_token`: JWT token (tự động lưu sau khi login)

### Cách thay đổi base_url:
1. Click chuột phải vào collection **"Novel Backend API"**
2. Chọn **Edit**
3. Vào tab **Variables**
4. Thay đổi giá trị `base_url` (ví dụ: `https://api.example.com`)

## Xác Thực (Authentication)

### Luồng Đăng Nhập:
1. **Register** (không cần token):
   - Endpoint: `POST /api/auth/register`
   - Body: `email`, `password`, `displayName`
   
2. **Login** (không cần token):
   - Endpoint: `POST /api/auth/login`
   - Body: `email`, `password`
   - ✨ Token tự động lưu vào biến `jwt_token`

3. Sau khi login, tất cả các API yêu cầu authentication sẽ tự động sử dụng token đã lưu

### API Không Cần Token (Public APIs):

#### ✅ Auth & Health
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/health`

#### ✅ Genres (Chỉ GET)
- `GET /api/genres` - Lấy tất cả thể loại
- `GET /api/genres/{id}` - Lấy thể loại theo ID
- `GET /api/genres/name/{name}` - Lấy thể loại theo tên

#### ✅ Stories (Chỉ GET)
- `GET /api/stories` - Lấy tất cả truyện
- `GET /api/stories/{id}` - Lấy truyện theo ID
- Tất cả search/filter endpoints

#### ✅ Chapters (Chỉ GET)
- `GET /api/stories/{storyId}/chapters` - Lấy tất cả chương
- `GET /api/stories/{storyId}/chapters/{id}` - Lấy chương cụ thể

#### ✅ Ratings (Chỉ GET public)
- `GET /api/ratings/story/{storyId}` - Lấy ratings của truyện
- `GET /api/ratings/story/{storyId}/average` - Lấy rating trung bình

#### ✅ Comments (Chỉ GET public)
- `GET /api/comments/{id}` - Lấy comment theo ID
- `GET /api/comments/story/{storyId}` - Lấy comments của truyện
- `GET /api/comments/story/{storyId}/count` - Đếm số comment

#### ✅ AI Services
- `POST /api/ai/search/semantic` - Tìm kiếm ngữ nghĩa (không cần token)
- `GET /api/ai/health` - Health check AI service

### API Yêu Cầu Token (Protected APIs):

#### 🔒 Genres (CUD Operations)
- `POST /api/genres` - Tạo thể loại mới
- `PUT /api/genres/{id}` - Cập nhật thể loại
- `DELETE /api/genres/{id}` - Xóa thể loại

#### 🔒 Stories (CUD Operations)
- `POST /api/stories` - Tạo truyện mới
- `PUT /api/stories/{id}` - Cập nhật truyện
- `DELETE /api/stories/{id}` - Xóa truyện
- `POST /api/stories/translate/*` - Dịch truyện

#### 🔒 Chapters (CUD Operations)
- `POST /api/stories/{storyId}/chapters` - Tạo chương mới
- `PUT /api/stories/{storyId}/chapters/{id}` - Cập nhật chương
- `DELETE /api/stories/{storyId}/chapters/{id}` - Xóa chương
- `PATCH /api/stories/{storyId}/chapters/{id}/*` - Cập nhật trạng thái
- `POST /api/stories/{storyId}/chapters/*/translate*` - Dịch chương

#### 🔒 Ratings
- `POST /api/ratings` - Tạo/cập nhật rating
- `PUT /api/ratings/{id}` - Cập nhật rating
- `DELETE /api/ratings/{id}` - Xóa rating
- `GET /api/ratings/user/me` - Lấy ratings của tôi
- `GET /api/ratings/story/{storyId}/me` - Lấy rating của tôi cho truyện

#### 🔒 Comments
- `POST /api/comments` - Tạo comment
- `PUT /api/comments/{id}` - Cập nhật comment
- `DELETE /api/comments/{id}` - Xóa comment
- `GET /api/comments/user/me` - Lấy comments của tôi

#### 🔒 Reading History
- `GET /api/history` - Lấy lịch sử đọc
- `POST /api/history` - Cập nhật tiến độ đọc

#### 🔒 Crawl Jobs
- Tất cả crawl job endpoints

#### 🔒 AI Services
- `POST /api/ai/translate` - Dịch văn bản
- `POST /api/ai/translate/auto` - Tự động dịch
- `POST /api/ai/embeddings/story/{id}` - Tạo embedding
- `POST /api/ai/embeddings/generate-all` - Tạo tất cả embeddings
- `PUT /api/ai/embeddings/story/{id}/refresh` - Refresh embedding

## Cấu Trúc Collection

### 1. Auth
- Register
- Login (auto-save token)

### 2. Health
- Health Check

### 3. Genres
- Get All Genres (public)
- Get Genre by ID (public)
- Get Genre by Name (public)
- Create Genre (protected)
- Update Genre (protected)
- Delete Genre (protected)

### 4. Stories
- Get All Stories (public)
- Search Stories by Keyword (public)
- Get Stories by Genre ID (public)
- Get Stories by Genre Name (public)
- Get Story by ID (public)
- Create Story (protected)
- Update Story (protected)
- Delete Story (protected)
- Translate Story (protected)
- Translate Story by ID (protected)

### 5. Chapters
- Get Chapters by Story ID (public)
- Get Chapter by ID (public)
- Create Chapter (protected)
- Update Chapter (protected)
- Delete Chapter (protected)
- Update Raw Content (protected)
- Update Translation (protected)
- Update Crawl Status (protected)
- Update Translate Status (protected)
- Translate Chapter (protected)
- Translate All Chapters (protected)
- Retry Failed Translations (protected)

### 6. Crawl Jobs
- Get All Jobs (protected)
- Get Jobs by Status (protected)
- Get Job by ID (protected)
- Create Job (protected)
- Update Job Status (protected)
- Delete Job (protected)
- Get Jobs by Story (protected)
- Get Jobs by Story and Type (protected)
- Get Jobs by Chapter (protected)

### 7. Ratings
- Create or Update Rating (protected)
- Update Rating (protected)
- Delete Rating (protected)
- Get Ratings by Story (public)
- Get Story Rating Average (public)
- Get My Ratings (protected)
- Get My Rating for Story (protected)

### 8. Comments
- Create Comment (protected)
- Update Comment (protected)
- Delete Comment (protected)
- Get Comment by ID (public)
- Get Comments by Story (public)
- Get Comment Count by Story (public)
- Get My Comments (protected)

### 9. Reading History
- Get Reading History (protected)
- Update Reading Progress (protected)

### 10. AI Services
- Translate Text (protected)
- Auto Translate (protected)
- Semantic Search (public)
- Generate Story Embedding (protected)
- Generate All Embeddings (protected)
- Refresh Story Embedding (protected)
- AI Health Check (public)

## Ví Dụ Sử Dụng

### 1. Đăng Ký và Đăng Nhập

```
1. POST /api/auth/register
   Body: {
     "email": "user@example.com",
     "password": "password123",
     "displayName": "Test User"
   }

2. POST /api/auth/login
   Body: {
     "email": "user@example.com",
     "password": "password123"
   }
   → Token tự động lưu
```

### 2. Tìm Kiếm Truyện (Không cần đăng nhập)

```
GET /api/stories?keyword=fantasy&page=0&size=20
GET /api/stories?genre=Action&page=0&size=20
GET /api/stories/1
```

### 3. Đọc Truyện (Không cần đăng nhập)

```
GET /api/stories/1/chapters
GET /api/stories/1/chapters/1
```

### 4. Rating và Comment (Cần đăng nhập)

```
POST /api/ratings
Body: {
  "storyId": 1,
  "rating": 5
}

POST /api/comments
Body: {
  "storyId": 1,
  "content": "Great story!"
}
```

### 5. Tìm Kiếm Ngữ Nghĩa với AI (Không cần đăng nhập)

```
POST /api/ai/search/semantic
Body: {
  "query": "romantic fantasy adventure",
  "limit": 10
}
```

### 6. Quản Lý Truyện (Cần đăng nhập)

```
POST /api/stories
Body: {
  "title": "My Novel",
  "author": "Author Name",
  "description": "Story description",
  "sourceUrl": "https://example.com",
  "genreId": 1
}

PUT /api/stories/1
Body: {
  "title": "Updated Title",
  "author": "Updated Author",
  "description": "Updated description",
  "genreId": 1
}
```

## Lưu Ý

1. **Token Expiration**: Nếu token hết hạn, bạn cần login lại
2. **Pagination**: Hầu hết GET endpoints hỗ trợ `page` và `size` parameters
3. **Error Handling**: Kiểm tra response status và error messages
4. **Rate Limiting**: Một số AI endpoints có thể có rate limiting

## Troubleshooting

### Token không tự động lưu?
- Kiểm tra Login request có script trong tab **Tests**
- Đảm bảo response trả về có field `accessToken`

### API trả về 401 Unauthorized?
- Kiểm tra xem đã login chưa
- Xem biến `jwt_token` đã có giá trị chưa
- Login lại nếu token đã hết hạn

### API trả về 403 Forbidden?
- User có thể chưa có quyền truy cập endpoint đó
- Kiểm tra role/permission của user

### Cannot connect to server?
- Kiểm tra server đã chạy chưa
- Kiểm tra `base_url` có đúng không
- Kiểm tra firewall/network settings

## Liên Hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ team phát triển.

