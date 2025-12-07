package com.graduate.novel.domain.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
    @NotBlank(message = "Role name is required")
    @Size(min = 1, max = 50, message = "Role name must be between 1 and 50 characters")
    String name,

    @Size(max = 255, message = "Description must be at most 255 characters")
    String description
) {}

