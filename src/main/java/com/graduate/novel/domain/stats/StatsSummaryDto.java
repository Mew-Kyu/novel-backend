package com.graduate.novel.domain.stats;

public record StatsSummaryDto(
    Long totalStories,
    Long totalGenres,
    Long totalChapters,
    Long totalUsers,
    Long totalViews
) {}

