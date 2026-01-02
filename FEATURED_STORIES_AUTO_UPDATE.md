# Featured Stories Auto-Update Documentation

## Tổng quan

Hệ thống tự động cập nhật featured stories dựa trên các chỉ số hiệu suất:
- **View count** (số lượt xem)
- **Rating** (đánh giá trung bình và số lượng đánh giá)
- **Recency** (thời gian cập nhật gần đây)

## Cách hoạt động

### 1. Scheduled Job
- **Mặc định**: Chạy hàng ngày lúc **2:00 AM**
- **Có thể cấu hình** thời gian chạy trong `application.yml`

### 2. Quy trình tự động

```
┌─────────────────────────────────────────────────┐
│  1. Reset tất cả featured hiện tại về FALSE     │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  2. Tìm stories đủ điều kiện:                   │
│     ✓ Updated trong 7 ngày gần đây              │
│     ✓ View count ≥ 100                          │
│     ✓ Rating ≥ 3.5 (nếu có rating)              │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  3. Tính điểm cho mỗi story:                    │
│     Score = (ViewCount × 0.4)                   │
│           + (Rating × 0.3)                      │
│           + (Recency × 0.3)                     │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  4. Chọn top 10 stories có điểm cao nhất        │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  5. Set featured = TRUE cho top 10              │
└─────────────────────────────────────────────────┘
```

### 3. Công thức tính điểm

```java
score = 0.0

// View count (40%)
score += (viewCount / 1000.0) × 0.4

// Rating (30%)
if (hasRating) {
    ratingScore = averageRating × log10(totalRatings + 1)
    score += ratingScore × 0.3
}

// Recency (30%)
daysSinceUpdate = currentDate - updatedAt
recencyScore = exp(-daysSinceUpdate / 7) × 10
score += recencyScore × 0.3
```

## Cấu hình

### File: `src/main/resources/application.yml`

```yaml
app:
  featured-stories:
    enabled: true                      # Bật/tắt auto-update
    max-count: 10                      # Số lượng featured stories tối đa
    min-days-since-update: 7           # Story phải được update trong N ngày gần đây
    min-view-count: 100                # View count tối thiểu
    min-rating: 3.5                    # Rating tối thiểu (nếu có rating)
    view-count-weight: 0.4             # Tỷ trọng view count (0.0 - 1.0)
    rating-weight: 0.3                 # Tỷ trọng rating (0.0 - 1.0)
    recency-weight: 0.3                # Tỷ trọng độ mới (0.0 - 1.0)
    cron-expression: "0 0 2 * * *"     # Thời gian chạy (daily 2 AM)
```

### Ý nghĩa cron expression:

```
"0 0 2 * * *"
 │ │ │ │ │ │
 │ │ │ │ │ └─── Day of week (0-7, 0=Sunday)
 │ │ │ │ └───── Month (1-12)
 │ │ │ └─────── Day of month (1-31)
 │ │ └───────── Hour (0-23)
 │ └─────────── Minute (0-59)
 └───────────── Second (0-59)
```

Ví dụ:
- `"0 0 2 * * *"` - Mỗi ngày lúc 2:00 AM
- `"0 0 */6 * * *"` - Mỗi 6 giờ một lần
- `"0 0 0 * * SUN"` - Mỗi Chủ nhật lúc 00:00

## API Endpoints

### 1. Lấy featured stories (Public)

```http
GET /api/stories/featured?limit=5
```

**Response:**
```json
[
  {
    "id": 1,
    "title": "Story Title",
    "viewCount": 1500,
    "averageRating": 4.5,
    "featured": true,
    ...
  }
]
```

### 2. Set featured thủ công (ADMIN only)

```http
PATCH /api/stories/{id}/featured?featured=true
Authorization: Bearer {admin_token}
```

**Response:**
```json
{
  "id": 1,
  "featured": true,
  ...
}
```

### 3. Trigger auto-update thủ công (ADMIN only)

```http
POST /api/stories/featured/refresh
Authorization: Bearer {admin_token}
```

**Response:**
```json
"Featured stories updated successfully"
```

## Testing

### 1. Kiểm tra scheduler có chạy không

Xem logs khi application khởi động:
```
INFO  c.g.n.s.FeaturedStoryScheduler : Starting scheduled featured stories update
INFO  c.g.n.s.FeaturedStoryScheduler : Reset 5 previously featured stories
INFO  c.g.n.s.FeaturedStoryScheduler : Featured stories update completed. Promoted 10 stories
```

### 2. Test thủ công

Gọi API refresh với ADMIN token:
```bash
curl -X POST "http://localhost:8080/api/stories/featured/refresh" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### 3. Kiểm tra kết quả

```bash
curl "http://localhost:8080/api/stories/featured?limit=10"
```

## Tắt auto-update

Nếu muốn tắt auto-update và chỉ set featured thủ công:

```yaml
app:
  featured-stories:
    enabled: false
```

Hoặc set environment variable:
```bash
export APP_FEATURED_STORIES_ENABLED=false
```

## Logging

Enable debug logging để xem chi tiết:

```yaml
logging:
  level:
    com.graduate.novel.scheduler: DEBUG
```

Log output:
```
DEBUG c.g.n.s.FeaturedStoryScheduler : Promoted story 'Epic Story' (ID: 5) - Views: 1500, Rating: 4.5, Score: 8.75
```

## Performance

- **Cache**: Featured stories được cache, auto-clear khi update
- **Transaction**: Tất cả operations chạy trong transaction
- **Batch**: Stories được save theo batch để tối ưu database calls

## Troubleshooting

### Featured stories không tự động update

1. Kiểm tra scheduler có enabled không:
```yaml
app.featured-stories.enabled: true
```

2. Kiểm tra `@EnableScheduling` trong `NovelApplication.java`

3. Xem logs có error không:
```bash
grep "FeaturedStoryScheduler" application.log
```

### Không có story nào được promote

Kiểm tra criteria có quá strict không:
- Giảm `min-view-count`
- Giảm `min-rating`
- Tăng `min-days-since-update`

### Score calculation không như mong đợi

Điều chỉnh weights trong config:
```yaml
view-count-weight: 0.5  # Tăng tỷ trọng view count
rating-weight: 0.3
recency-weight: 0.2     # Giảm tỷ trọng độ mới
```

## Migration Guide

Nếu upgrade từ version cũ, đảm bảo:

1. Database đã có column `featured`:
```sql
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'stories' AND column_name = 'featured';
```

2. Run migration nếu chưa có:
```sql
ALTER TABLE stories ADD COLUMN featured BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_stories_featured ON stories(featured) WHERE featured = true;
```

3. Initial data - mark một số stories là featured:
```sql
UPDATE stories 
SET featured = true 
WHERE id IN (
    SELECT id FROM stories 
    ORDER BY view_count DESC 
    LIMIT 10
);
```

