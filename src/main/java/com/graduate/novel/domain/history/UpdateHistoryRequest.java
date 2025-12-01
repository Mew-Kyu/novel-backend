package com.graduate.novel.domain.history;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateHistoryRequest(
    @NotNull(message = "Story ID is required")
    Long storyId,

    Long chapterId,

    @Min(value = 0, message = "Progress percent must be between 0 and 100")
    @Max(value = 100, message = "Progress percent must be between 0 and 100")
    Integer progressPercent,

    @Min(value = 0, message = "Scroll offset must be non-negative")
    Integer scrollOffset
) {}
