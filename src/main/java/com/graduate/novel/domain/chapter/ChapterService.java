package com.graduate.novel.domain.chapter;

import com.graduate.novel.ai.service.TranslationService;
import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.ChapterMapper;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        log.info("Starting translation for chapter {} of story {}", chapterId, storyId);

        try {
            // Set status to PENDING
            chapter.setTranslateStatus("PENDING");
            chapterRepository.save(chapter);

            // Perform translation
            String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

            // Update chapter with translation
            chapter.setTranslatedContent(translatedContent);
            chapter.setTranslateStatus("SUCCESS");
            chapter.setTranslateTime(LocalDateTime.now());

            chapter = chapterRepository.save(chapter);
            log.info("Successfully translated chapter {} of story {}", chapterId, storyId);

            return chapterMapper.toDto(chapter);

        } catch (Exception e) {
            log.error("Failed to translate chapter {} of story {}: {}", chapterId, storyId, e.getMessage());
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

        log.info("Found {} chapters to translate for story {}", chapters.size(), storyId);

        for (Chapter chapter : chapters) {
            try {
                log.info("Translating chapter {} (index: {})", chapter.getId(), chapter.getChapterIndex());

                chapter.setTranslateStatus("PENDING");
                chapterRepository.save(chapter);

                String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

                chapter.setTranslatedContent(translatedContent);
                chapter.setTranslateStatus("SUCCESS");
                chapter.setTranslateTime(LocalDateTime.now());
                chapterRepository.save(chapter);

                log.info("Successfully translated chapter {}", chapter.getId());

                // Add small delay to avoid rate limiting
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("Failed to translate chapter {}: {}", chapter.getId(), e.getMessage());
                chapter.setTranslateStatus("FAILED");
                chapter.setTranslateTime(LocalDateTime.now());
                chapterRepository.save(chapter);
            }
        }

        log.info("Completed translation for story {}", storyId);
    }

    /**
     * Retry failed translations for a story
     */
    @Transactional
    public void retryFailedTranslations(Long storyId) {
        List<Chapter> failedChapters = chapterRepository.findByStoryIdAndTranslateStatus(storyId, "FAILED");

        log.info("Retrying {} failed translations for story {}", failedChapters.size(), storyId);

        for (Chapter chapter : failedChapters) {
            try {
                if (chapter.getRawContent() != null && !chapter.getRawContent().trim().isEmpty()) {
                    chapter.setTranslateStatus("PENDING");
                    chapterRepository.save(chapter);

                    String translatedContent = translationService.translateToVietnamese(chapter.getRawContent());

                    chapter.setTranslatedContent(translatedContent);
                    chapter.setTranslateStatus("SUCCESS");
                    chapter.setTranslateTime(LocalDateTime.now());
                    chapterRepository.save(chapter);

                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("Retry failed for chapter {}: {}", chapter.getId(), e.getMessage());
                chapter.setTranslateStatus("FAILED");
                chapterRepository.save(chapter);
            }
        }
    }
}
