package com.graduate.novel.domain.history;

import com.graduate.novel.domain.story.StoryDto;
import java.time.LocalDateTime;

public record ReadingHistoryDto(
    Long id,
    Long userId,
    StoryDto story,
    Long chapterId,
    String chapterTitle,
    Integer progressPercent,
    Integer scrollOffset,
    LocalDateTime lastReadAt
) {}
