package com.graduate.novel.domain.genre;

import jakarta.validation.constraints.Size;

public record UpdateGenreRequest(
        @Size(min = 1, max = 50, message = "Genre name must be between 1 and 50 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {}

