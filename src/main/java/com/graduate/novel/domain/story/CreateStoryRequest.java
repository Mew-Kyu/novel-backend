package com.graduate.novel.domain.story;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoryRequest(
    @NotBlank(message = "Title is required")
    String title,

    String authorName,

    String description,

    String coverImageUrl,

    String sourceUrl,

    @Size(max = 255, message = "Source site must not exceed 255 characters")
    String sourceSite
) {}
