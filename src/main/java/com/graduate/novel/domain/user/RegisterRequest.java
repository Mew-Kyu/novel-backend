package com.graduate.novel.domain.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 255, message = "Display name must be between 1 and 255 characters")
    String displayName,

    // Optional role name, defaults to USER if not specified
    String roleName
) {}
