package com.graduate.novel.domain.crawljob;

import java.time.LocalDateTime;

public record CrawlJobDto(
    Long id,
    Long storyId,
    Long chapterId,
    String jobType,
    String status,
    Integer attempts,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

