package com.graduate.novel.domain.chapter;

import com.graduate.novel.ai.service.TranslationService;
import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.ChapterMapper;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;
    private final ChapterMapper chapterMapper;
    private final TranslationService translationService;

    @Transactional(readOnly = true)
    public List<ChapterDto> getChaptersByStoryId(Long storyId) {
        if (!storyRepository.existsById(storyId)) {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        }

        List<Chapter> chapters = chapterRepository.findByStoryIdOrderByChapterIndexAsc(storyId);
        return chapters.stream()
                .map(chapterMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChapterDto getChapterById(Long storyId, Long chapterId) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        return chapterMapper.toDto(chapter);
    }

    @Transactional
    public ChapterDto createChapter(Long storyId, CreateChapterRequest request) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        if (chapterRepository.existsByStoryIdAndChapterIndex(storyId, request.chapterIndex())) {
            throw new BadRequestException("Chapter index already exists for this story");
        }

        Chapter chapter = chapterMapper.toEntity(request);
        chapter.setStory(story);

        chapter = chapterRepository.save(chapter);
        return chapterMapper.toDto(chapter);
    }

    @Transactional
    public ChapterDto updateChapter(Long storyId, Long chapterId, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        chapterMapper.updateEntity(request, chapter);
        chapter = chapterRepository.save(chapter);
        return chapterMapper.toDto(chapter);
    }

    @Transactional
    public void deleteChapter(Long storyId, Long chapterId) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
        chapterRepository.delete(chapter);
    }

    @Transactional
    public ChapterDto updateRawContent(Long storyId, Long chapterId, String rawContent) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        chapter.setRawContent(rawContent);
        chapter.setCrawlStatus("SUCCESS");
        chapter.setCrawlTime(LocalDateTime.now());

        chapter = chapterRepository.save(chapter);
        return chapterMapper.toDto(chapter);
    }

    @Transactional
    public ChapterDto updateTranslation(Long storyId, Long chapterId, String translatedContent) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        chapter.setTranslatedContent(translatedContent);
        chapter.setTranslateStatus("SUCCESS");
        chapter.setTranslateTime(LocalDateTime.now());

        chapter = chapterRepository.save(chapter);
        return chapterMapper.toDto(chapter);
    }

    @Transactional
    public void updateCrawlStatus(Long chapterId, String status, String errorMessage) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        chapter.setCrawlStatus(status);
        if ("FAILED".equals(status) && errorMessage != null) {
            // Could store error in a separate field if needed
        }
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            chapter.setCrawlTime(LocalDateTime.now());
        }

        chapterRepository.save(chapter);
    }

    @Transactional
    public void updateTranslateStatus(Long chapterId, String status, String errorMessage) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        chapter.setTranslateStatus(status);
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            chapter.setTranslateTime(LocalDateTime.now());
        }

        chapterRepository.save(chapter);
    }

    @Transactional(readOnly = true)
    public List<ChapterDto> getChaptersByCrawlStatus(String status) {
        return chapterRepository.findByCrawlStatus(status).stream()
                .map(chapterMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChapterDto> getChaptersByTranslateStatus(String status) {
        return chapterRepository.findByTranslateStatus(status).stream()
                .map(chapterMapper::toDto)
                .toList();
    }

    /**
     * Automatically translate a chapter using Gemini AI
     */
    @Transactional
    public ChapterDto translateChapter(Long storyId, Long chapterId) {
        Chapter chapter = chapterRepository.findByStoryIdAndId(storyId, chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        if (chapter.getRawContent() == null || chapter.getRawContent().trim().isEmpty()) {
            throw new BadRequestException("Cannot translate: Chapter has no raw content");
        }

        log.info("Starting translation for chapterId={} (index={}) of storyId={}",
                chapterId, chapter.getChapterIndex(), storyId);

        try {
            // Set status to PENDING
            chapter.setTranslateStatus("PENDING");
            chapterRepository.save(chapter);

            // Translate chapter title if raw title exists
            if (chapter.getRawTitle() != null && !chapter.getRawTitle().trim().isEmpty()) {
                log.info("Translating chapter title for chapterId={} (index={})",
                        chapterId, chapter.getChapterIndex());
                String translatedTitle = translationService.translateToVietnamese(chapter.getRawTitle());
                chapter.setTranslatedTitle(translatedTitle);
                // Also update the main title field for backward compatibility
                chapter.setTitle(translatedTitle);
                log.info("Chapter title translated successfully: '{}' -> '{}'",
                        chapter.getRawTitle(), translatedTitle);
            }

            // Perform translation of content
            String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

            // Update chapter with translation
            chapter.setTranslatedContent(translatedContent);
            chapter.setTranslateStatus("SUCCESS");
            chapter.setTranslateTime(LocalDateTime.now());

            chapter = chapterRepository.save(chapter);
            log.info("✅ Successfully translated chapterId={} (index={}) of storyId={}",
                    chapterId, chapter.getChapterIndex(), storyId);

            return chapterMapper.toDto(chapter);

        } catch (Exception e) {
            log.error("❌ Failed to translate chapterId={} (index={}) of storyId={}: {}",
                    chapterId, chapter.getChapterIndex(), storyId, e.getMessage());
            chapter.setTranslateStatus("FAILED");
            chapter.setTranslateTime(LocalDateTime.now());
            chapterRepository.save(chapter);
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Translate all chapters of a story that have raw content but no translation
     */
    @Transactional
    public void translateAllChaptersForStory(Long storyId) {
        if (!storyRepository.existsById(storyId)) {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        }

        List<Chapter> chapters = chapterRepository.findByStoryIdAndTranslateStatusAndRawContentIsNotNull(
                storyId, "NONE");

        log.info("Found {} chapters to translate for storyId={}", chapters.size(), storyId);

        for (Chapter chapter : chapters) {
            try {
                log.info("Translating chapterId={} (index={}) of storyId={}",
                        chapter.getId(), chapter.getChapterIndex(), storyId);

                chapter.setTranslateStatus("PENDING");
                chapterRepository.save(chapter);

                // Translate chapter title if raw title exists
                if (chapter.getRawTitle() != null && !chapter.getRawTitle().trim().isEmpty()) {
                    String translatedTitle = translationService.translateToVietnamese(chapter.getRawTitle());
                    chapter.setTranslatedTitle(translatedTitle);
                    chapter.setTitle(translatedTitle);
                }

                String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

                chapter.setTranslatedContent(translatedContent);
                chapter.setTranslateStatus("SUCCESS");
                chapter.setTranslateTime(LocalDateTime.now());
                chapterRepository.save(chapter);

                log.info("✅ Successfully translated chapterId={} (index={})",
                        chapter.getId(), chapter.getChapterIndex());

                // Add small delay to avoid rate limiting
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("❌ Failed to translate chapterId={} (index={}): {}",
                        chapter.getId(), chapter.getChapterIndex(), e.getMessage());
                chapter.setTranslateStatus("FAILED");
                chapter.setTranslateTime(LocalDateTime.now());
                chapterRepository.save(chapter);
            }
        }

        log.info("Completed translation for storyId={}", storyId);
    }

    /**
     * Retry failed translations for a story
     */
    @Transactional
    public void retryFailedTranslations(Long storyId) {
        List<Chapter> failedChapters = chapterRepository.findByStoryIdAndTranslateStatus(storyId, "FAILED");

        log.info("Retrying {} failed translations for storyId={}", failedChapters.size(), storyId);

        for (Chapter chapter : failedChapters) {
            try {
                if (chapter.getRawContent() != null && !chapter.getRawContent().trim().isEmpty()) {
                    log.info("Retrying chapterId={} (index={})", chapter.getId(), chapter.getChapterIndex());

                    chapter.setTranslateStatus("PENDING");
                    chapterRepository.save(chapter);

                    // Translate chapter title if raw title exists
                    if (chapter.getRawTitle() != null && !chapter.getRawTitle().trim().isEmpty()) {
                        String translatedTitle = translationService.translateToVietnamese(chapter.getRawTitle());
                        chapter.setTranslatedTitle(translatedTitle);
                        chapter.setTitle(translatedTitle);
                    }

                    String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

                    chapter.setTranslatedContent(translatedContent);
                    chapter.setTranslateStatus("SUCCESS");
                    chapter.setTranslateTime(LocalDateTime.now());
                    chapterRepository.save(chapter);

                    log.info("✅ Retry succeeded for chapterId={} (index={})",
                            chapter.getId(), chapter.getChapterIndex());

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("❌ Retry failed for chapterId={} (index={}): {}",
                        chapter.getId(), chapter.getChapterIndex(), e.getMessage());
                chapter.setTranslateStatus("FAILED");
                chapterRepository.save(chapter);
            }
        }
    }

    // ========== Homepage Features ==========

    /**
     * Get latest chapters across all stories
     */
    @Transactional(readOnly = true)
    public List<LatestChapterDto> getLatestChapters(int limit) {
        List<Chapter> chapters = chapterRepository.findLatestChapters(PageRequest.of(0, limit));

        return chapters.stream()
                .map(chapter -> new LatestChapterDto(
                        chapter.getId(),
                        chapter.getStory().getId(),
                        chapter.getStory().getTitle(),
                        chapter.getStory().getTranslatedTitle(),
                        chapter.getChapterIndex(),
                        chapter.getTitle(),
                        chapter.getTranslatedTitle(),
                        chapter.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }
}
