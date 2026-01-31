# Fix for ClientAbortException During Translation

## Problem
When translating chapters, the application was experiencing `ClientAbortException: java.io.IOException: An established connection was aborted by the software in your host machine`. This error occurred when:
1. The client disconnects before the server finishes processing the translation
2. The translation takes too long and the client times out
3. Large responses cause connection issues

## Root Causes
1. **Synchronous translation endpoint** - The `/translate` endpoint was blocking and waiting for the full translation to complete before responding, which could take several minutes
2. **Client timeout** - Browser/client would timeout before the translation completed
3. **No graceful handling** - ClientAbortException was being logged as errors even though it's often expected behavior

## Solutions Implemented

### 1. Async Translation for Single Chapters
**File**: `ChapterController.java`

Changed the `/api/stories/{storyId}/chapters/{chapterId}/translate` endpoint to run asynchronously:
- Returns immediately with `202 Accepted` status
- Runs translation in a background thread
- Client doesn't need to wait for completion
- Response includes status information

```java
@PostMapping("/{chapterId}/translate")
public ResponseEntity<Map<String, Object>> translateChapter(...) {
    // Run translation in background to avoid client timeout
    new Thread(() -> {
        try {
            chapterService.translateChapter(storyId, chapterId);
        } catch (Exception e) {
            log.error("Background translation failed for chapterId={}: {}", chapterId, e.getMessage());
        }
    }).start();
    
    return ResponseEntity.accepted().body(response);
}
```

### 2. Increased Timeout for Gemini API
**File**: `application.yml`

Increased timeout from 90 seconds to 180 seconds (3 minutes):
```yaml
gemini:
  api:
    timeout: 180000  # 3 minutes
```

This allows for longer translations without timing out on the API side.

### 3. Better Network Error Handling
**File**: `GeminiService.java`

Added specific handling for `ResourceAccessException` which includes connection timeouts and client disconnects:
- Detects timeout and connection abort scenarios
- Implements retry logic with exponential backoff
- Provides better error messages
- Prevents cascading failures

```java
catch (org.springframework.web.client.ResourceAccessException e) {
    if (e.getMessage() != null && (e.getMessage().contains("timeout") || 
        e.getMessage().contains("connection was aborted"))) {
        // Retry with exponential backoff
    }
}
```

### 4. Global Exception Handler for Client Disconnects
**File**: `GlobalExceptionHandler.java`

Added handlers to gracefully handle client disconnections:
- `ClientAbortException` - Logs at DEBUG level instead of ERROR
- `IOException` for connection errors - Distinguishes between client disconnects and real errors
- Prevents error spam in logs for expected behavior

```java
@ExceptionHandler(ClientAbortException.class)
public void handleClientAbortException(ClientAbortException ex) {
    log.debug("Client aborted connection: {}", ex.getMessage());
    // No response needed as client already disconnected
}
```

### 5. Improved RestTemplate Configuration
**File**: `RestTemplateConfig.java`

Simplified configuration using `SimpleClientHttpRequestFactory`:
- Removed deprecated methods
- Clean timeout configuration
- Better compatibility with Spring Boot 3.x

## Benefits

1. **No more client timeouts** - Clients get immediate response and can poll for status
2. **Better user experience** - Users don't have to wait with browser hanging
3. **Cleaner logs** - Client disconnects logged appropriately, not as errors
4. **More resilient** - Retry logic handles temporary network issues
5. **Scalable** - Background processing allows handling multiple translations

## How It Works Now

### Translation Flow:
1. Client sends POST to `/api/stories/{storyId}/chapters/{chapterId}/translate`
2. Server immediately returns `202 Accepted` with:
   ```json
   {
     "message": "Translation started for chapter",
     "storyId": 123,
     "chapterId": 456,
     "status": "PENDING"
   }
   ```
3. Translation runs in background
4. Client can poll chapter status via GET endpoint to check progress
5. Chapter status updates from `PENDING` → `SUCCESS` or `FAILED`

### Error Handling:
- Network errors retry up to 3 times with exponential backoff
- Client disconnects are logged at DEBUG level
- Real errors still logged at ERROR level with full stack traces
- Translation failures update chapter status to `FAILED`

## Testing

After applying these changes:
1. Start a translation and close the browser - no errors in logs
2. Translate a large chapter - completes successfully in background
3. Multiple simultaneous translations - all process correctly
4. Network blip during translation - retries automatically

## Additional Notes

- The `/translate-all` endpoint already uses this async pattern
- Consider implementing WebSocket or Server-Sent Events for real-time status updates
- Monitor background thread pool if processing many translations simultaneously
- Consider using `@Async` with proper thread pool configuration for production
