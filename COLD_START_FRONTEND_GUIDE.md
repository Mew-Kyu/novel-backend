# 🚀 Cold-Start & Onboarding — Frontend Integration Guide

> **Updated:** February 22, 2026  
> Áp dụng sau khi backend đã được fix 4 vấn đề cold-start.

---

## 1. Những thay đổi trong API (Breaking Changes)

### `POST /api/auth/register` và `POST /api/auth/login`

**AuthResponse trả về thêm 2 trường mới:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "user": {
    "id": 42,
    "email": "user@example.com",
    "displayName": "Nguyen Van A",
    "avatarUrl": null,
    "createdAt": "2026-02-22T10:00:00",
    "active": true,
    "role": { "id": 1, "name": "USER" }
  },
  "isColdStart": true,
  "onboardingRequired": true
}
```

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `isColdStart` | `boolean` | `true` = user chưa có đủ tương tác, cần dùng cold-start recommendations |
| `onboardingRequired` | `boolean` | `true` = user chưa hoàn thành onboarding preferences |

---

## 2. Luồng xử lý sau Login/Register

```
POST /api/auth/login (hoặc /register)
│
└── Đọc response:
    ├── Lưu accessToken + refreshToken
    ├── Lưu user vào state
    │
    ├── if (onboardingRequired === true)
    │   └── → Chuyển đến trang Onboarding (bắt buộc cho lần đầu)
    │         Sau khi submit → POST /api/onboarding/preferences
    │         → Sau đó tiếp tục bình thường
    │
    └── if (isColdStart === true && onboardingRequired === false)
        └── → Trang chủ dùng cold-start recommendations
              GET /api/recommendations/cold-start?limit=10
              (thay vì GET /api/recommendations/for-you)
```

---

## 3. Chi tiết từng Endpoint

### 3.1 Lưu Onboarding Preferences

```
POST /api/onboarding/preferences
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "preferredGenreIds": [1, 3, 7],
  "readingFrequency": "DAILY",
  "preferredLength": "MEDIUM",
  "completionPreference": "COMPLETIONIST",
  "explorationPreference": "BALANCED"
}
```

**Enum values:**

| Field | Values |
|-------|--------|
| `readingFrequency` | `DAILY`, `WEEKLY`, `MONTHLY`, `CASUAL` |
| `preferredLength` | `SHORT`, `MEDIUM`, `LONG`, `EPIC`, `ANY` |
| `completionPreference` | `COMPLETIONIST`, `EXPLORER`, `BALANCED` |
| `explorationPreference` | `SAFE`, `ADVENTUROUS`, `BALANCED` |

Response: `UserOnboarding` object

---

### 3.2 Kiểm tra trạng thái Onboarding

```
GET /api/onboarding/status
Authorization: Bearer <accessToken>
```

```json
{
  "completed": false,
  "onboarding": {
    "id": 1,
    "userId": 42,
    "preferredGenres": null,
    "completed": false,
    "createdAt": "2026-02-22T10:00:00"
  }
}
```

---

### 3.3 Cold-Start Recommendations

```
GET /api/recommendations/cold-start?limit=10
Authorization: Bearer <accessToken>
```

Response: `StoryDto[]` — danh sách truyện gợi ý cho user mới (trending + high-rated).

---

### 3.4 Kiểm tra trạng thái Cold-Start

```
GET /api/recommendations/cold-start/check
Authorization: Bearer <accessToken>
```

```json
{
  "isColdStart": true,
  "recommendedStrategy": "NewUserStrategy"
}
```

> **Lưu ý:** Không cần gọi endpoint này nữa sau login vì thông tin đã có trong `AuthResponse`.  
> Chỉ gọi khi cần check lại tại runtime (ví dụ: sau mỗi 10 lần đọc truyện).

---

### 3.5 Recommendations cho user đã có dữ liệu

```
GET /api/recommendations/for-you?limit=10
Authorization: Bearer <accessToken>
```

Dùng khi `isColdStart === false`.

---

## 4. Ví dụ code (TypeScript / React)

### 4.1 Sau khi login/register

```typescript
interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
  isColdStart: boolean;
  onboardingRequired: boolean;
}

async function handleAuthSuccess(response: AuthResponse) {
  // 1. Lưu tokens
  localStorage.setItem('accessToken', response.accessToken);
  localStorage.setItem('refreshToken', response.refreshToken);
  
  // 2. Lưu user state
  setCurrentUser(response.user);
  setIsColdStart(response.isColdStart);

  // 3. Điều hướng
  if (response.onboardingRequired) {
    router.push('/onboarding');          // Trang chọn sở thích
  } else {
    router.push('/');                    // Trang chủ
  }
}
```

---

### 4.2 Trang chủ — chọn đúng API

```typescript
async function fetchHomeRecommendations(isColdStart: boolean): Promise<StoryDto[]> {
  if (isColdStart) {
    // User mới: trending + high-rated
    const res = await api.get('/recommendations/cold-start?limit=10');
    return res.data;
  } else {
    // User cũ: hybrid personalized
    const res = await api.get('/recommendations/for-you?limit=10');
    return res.data.stories;
  }
}
```

---

### 4.3 Onboarding Page

```typescript
async function submitOnboarding(formData: OnboardingFormData) {
  await api.post('/onboarding/preferences', {
    preferredGenreIds: formData.selectedGenres,
    readingFrequency: formData.frequency,     // e.g. "DAILY"
    preferredLength: formData.length,          // e.g. "MEDIUM"
    completionPreference: formData.completion, // e.g. "BALANCED"
    explorationPreference: formData.explore,   // e.g. "ADVENTUROUS"
  });

  // Sau khi lưu onboarding → vẫn dùng cold-start cho trang chủ
  // vì user chưa có reading history. isColdStart check lại nếu cần.
  setOnboardingRequired(false);
  router.push('/');
}
```

---

### 4.4 Kiểm tra định kỳ để thoát cold-start

```typescript
// Gọi sau mỗi session / sau vài lần đọc truyện
async function refreshColdStartStatus() {
  const res = await api.get('/recommendations/cold-start/check');
  setIsColdStart(res.data.isColdStart);
  // Khi isColdStart = false → trang chủ sẽ tự dùng /for-you
}
```

---

## 5. Sơ đồ trạng thái đầy đủ

```
┌─────────────────────────────────────────────────────────────┐
│                    User truy cập app                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
           ┌───────────▼───────────┐
           │   Chưa đăng nhập?     │
           └───────────┬───────────┘
                       │ YES
           ┌───────────▼───────────┐
           │  POST /auth/login     │
           │  hoặc /auth/register  │
           └───────────┬───────────┘
                       │
          ┌────────────▼────────────┐
          │  onboardingRequired?    │
          └────┬──────────┬─────────┘
             YES          NO
              │            │
    ┌─────────▼──┐   ┌─────▼──────────────────┐
    │ /onboarding│   │  isColdStart?           │
    │  (genre,   │   └─────┬──────────┬────────┘
    │  frequency │       YES          NO
    │  etc.)     │         │           │
    └─────┬──────┘         │           │
          │         ┌──────▼──┐  ┌─────▼──────┐
          │         │ cold-   │  │ /for-you   │
          │         │ start   │  │ (hybrid    │
          └────────►│ recs    │  │  recommend)│
                    └────┬────┘  └────────────┘
                         │
           ┌─────────────▼─────────────┐
           │ Sau 10+ interactions:     │
           │ GET /cold-start/check     │
           │ → isColdStart = false     │
           │ → Chuyển sang /for-you    │
           └───────────────────────────┘
```

---

## 6. Checklist cho Frontend Team

- [ ] Cập nhật `AuthResponse` interface thêm `isColdStart` và `onboardingRequired`
- [ ] Sau login/register: đọc `onboardingRequired` → redirect nếu `true`
- [ ] Trang chủ: dùng `isColdStart` để chọn `/cold-start` hoặc `/for-you`
- [ ] Trang Onboarding: submit `POST /api/onboarding/preferences` với đúng enum values
- [ ] Lưu `isColdStart` vào state/context (không cần gọi lại `/cold-start/check` ngay)
- [ ] Định kỳ kiểm tra lại cold-start status sau mỗi session hoặc sau nhiều tương tác
- [ ] Xử lý trường hợp `isColdStart = true` nhưng `onboardingRequired = false` (user đã onboard, vẫn ít tương tác)

---

## 7. Tóm tắt thay đổi Backend đã fix

| # | Vấn đề | Fix |
|---|--------|-----|
| 1 | `AuthResponse` không có cold-start info | Thêm `isColdStart` + `onboardingRequired` |
| 2 | Không tự tạo `UserOnboarding` khi register | `AuthService.register()` auto-create record |
| 3 | `NewItemStrategy.isApplicable()` luôn `true` | Dùng `countByCreatedAtAfter()` thay vì `count()` |
| 4 | Cold-start ngưỡng quá thấp (3 tương tác) | Tăng `MAX_INTERACTIONS` từ 3 → 10 |

