package com.graduate.novel.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Display name is required")
        @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
        String displayName
) {
}
