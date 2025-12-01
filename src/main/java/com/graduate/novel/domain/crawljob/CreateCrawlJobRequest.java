package com.graduate.novel.domain.crawljob;

import jakarta.validation.constraints.NotBlank;

public record CreateCrawlJobRequest(
    Long storyId,
    Long chapterId,

    @NotBlank(message = "Job type is required")
    String jobType // CRAWL or TRANSLATE
) {}

