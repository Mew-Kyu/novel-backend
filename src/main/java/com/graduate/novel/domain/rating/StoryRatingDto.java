package com.graduate.novel.domain.rating;

public record StoryRatingDto(
        Long storyId,
        Double averageRating,
        Long totalRatings
) {}

