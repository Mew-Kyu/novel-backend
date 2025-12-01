package com.graduate.novel.domain.chapter;

import java.time.LocalDateTime;

public record ChapterDto(
    Long id,
    Long storyId,
    Integer chapterIndex,
    String title,
    String rawContent,
    String crawlStatus,
    LocalDateTime crawlTime,
    String translatedContent,
    String translateStatus,
    LocalDateTime translateTime,
    LocalDateTime createdAt
) {}
