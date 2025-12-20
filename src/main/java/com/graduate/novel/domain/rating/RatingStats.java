package com.graduate.novel.domain.rating;

/**
 * DTO for efficient rating statistics query
 * Returns both average and count in a single query
 */
public record RatingStats(
        Double averageRating,
        Long totalRatings
) {
    public RatingStats {
        // Handle null values
        if (totalRatings == null) {
            totalRatings = 0L;
        }
    }
}

