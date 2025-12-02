package com.graduate.novel.domain.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull(message = "Story ID is required")
        Long storyId,

        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
        String content
) {}

