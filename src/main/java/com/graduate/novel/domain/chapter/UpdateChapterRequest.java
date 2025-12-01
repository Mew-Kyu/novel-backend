package com.graduate.novel.domain.chapter;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateChapterRequest(
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,

    String rawContent,

    String translatedContent,

    @Positive(message = "Chapter index must be positive")
    Integer chapterIndex
) {}
