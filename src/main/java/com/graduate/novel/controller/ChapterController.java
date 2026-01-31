package com.graduate.novel.controller;

import com.graduate.novel.domain.chapter.ChapterDto;
import com.graduate.novel.domain.chapter.ChapterService;
import com.graduate.novel.domain.chapter.CreateChapterRequest;
import com.graduate.novel.domain.chapter.UpdateChapterRequest;
import com.graduate.novel.domain.chapter.UpdateChapterTranslationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories/{storyId}/chapters")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public ResponseEntity<List<ChapterDto>> getChaptersByStoryId(@PathVariable Long storyId) {
        List<ChapterDto> chapters = chapterService.getChaptersByStoryId(storyId);
        return ResponseEntity.ok(chapters);
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<ChapterDto> getChapterById(
            @PathVariable Long storyId,
            @PathVariable Long chapterId
    ) {
        ChapterDto chapter = chapterService.getChapterById(storyId, chapterId);
        return ResponseEntity.ok(chapter);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ChapterDto> createChapter(
            @PathVariable Long storyId,
            @Valid @RequestBody CreateChapterRequest request
    ) {
        ChapterDto chapter = chapterService.createChapter(storyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(chapter);
    }

    @PutMapping("/{chapterId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<ChapterDto> updateChapter(
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @Valid @RequestBody UpdateChapterRequest request
    ) {
        ChapterDto chapter = chapterService.updateChapter(storyId, chapterId, request);
        return ResponseEntity.ok(chapter);
    }

    @DeleteMapping("/{chapterId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long storyId,
            @PathVariable Long chapterId
    ) {
        chapterService.deleteChapter(storyId, chapterId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{chapterId}/raw-content")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<ChapterDto> updateRawContent(
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @RequestBody Map<String, String> body
    ) {
        String rawContent = body.get("rawContent");
        ChapterDto chapter = chapterService.updateRawContent(storyId, chapterId, rawContent);
        return ResponseEntity.ok(chapter);
    }

    @PatchMapping("/{chapterId}/translation")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<ChapterDto> updateTranslation(
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @Valid @RequestBody UpdateChapterTranslationRequest request
    ) {
        ChapterDto chapter = chapterService.updateTranslation(storyId, chapterId, request.translatedContent());
        return ResponseEntity.ok(chapter);
    }

    @PatchMapping("/{chapterId}/crawl-status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<Void> updateCrawlStatus(
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String errorMessage = body.get("errorMessage");
        chapterService.updateCrawlStatus(chapterId, status, errorMessage);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{chapterId}/translate-status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<Void> updateTranslateStatus(
            @PathVariable Long storyId,
            @PathVariable Long chapterId,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String errorMessage = body.get("errorMessage");
        chapterService.updateTranslateStatus(chapterId, status, errorMessage);
        return ResponseEntity.ok().build();
    }

    /**
     * Automatically translate a chapter using AI
     * Returns immediately with 202 Accepted and runs translation in background
     */
    @PostMapping("/{chapterId}/translate")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyChapter(authentication, #storyId, #chapterId))")
    public ResponseEntity<Map<String, Object>> translateChapter(
            @PathVariable Long storyId,
            @PathVariable Long chapterId
    ) {
        // Run translation in background to avoid client timeout
        new Thread(() -> {
            try {
                chapterService.translateChapter(storyId, chapterId);
            } catch (Exception e) {
                log.error("Background translation failed for chapterId={}: {}", chapterId, e.getMessage());
            }
        }).start();

        Map<String, Object> response = Map.of(
                "message", "Translation started for chapter",
                "storyId", storyId,
                "chapterId", chapterId,
                "status", "PENDING"
        );
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Translate all untranslated chapters for a story
     */
    @PostMapping("/translate-all")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyStory(authentication, #storyId))")
    public ResponseEntity<Map<String, String>> translateAllChapters(@PathVariable Long storyId) {
        // Run in background to avoid timeout
        new Thread(() -> chapterService.translateAllChaptersForStory(storyId)).start();

        Map<String, String> response = Map.of(
                "message", "Translation started for all chapters",
                "storyId", storyId.toString()
        );
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Retry failed translations
     */
    @PostMapping("/retry-failed-translations")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MODERATOR') and @securityExpressionHandler.canModifyStory(authentication, #storyId))")
    public ResponseEntity<Map<String, String>> retryFailedTranslations(@PathVariable Long storyId) {
        new Thread(() -> chapterService.retryFailedTranslations(storyId)).start();

        Map<String, String> response = Map.of(
                "message", "Retrying failed translations",
                "storyId", storyId.toString()
        );
        return ResponseEntity.accepted().body(response);
    }
}
