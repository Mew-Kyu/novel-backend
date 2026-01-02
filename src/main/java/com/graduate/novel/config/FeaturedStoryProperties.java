package com.graduate.novel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for featured stories auto-update
 */
@Configuration
@ConfigurationProperties(prefix = "app.featured-stories")
@Data
public class FeaturedStoryProperties {

    /**
     * Maximum number of stories to feature at once
     */
    private int maxCount = 10;

    /**
     * Minimum number of days since last update to be considered
     */
    private int minDaysSinceUpdate = 7;

    /**
     * Minimum view count to be eligible for featured
     */
    private long minViewCount = 100;

    /**
     * Minimum average rating to be eligible (if story has ratings)
     */
    private double minRating = 3.5;

    /**
     * Weight for view count in scoring (0.0 to 1.0)
     */
    private double viewCountWeight = 0.4;

    /**
     * Weight for rating in scoring (0.0 to 1.0)
     */
    private double ratingWeight = 0.3;

    /**
     * Weight for recency in scoring (0.0 to 1.0)
     */
    private double recencyWeight = 0.3;

    /**
     * Enable/disable automatic featured stories update
     */
    private boolean enabled = true;

    /**
     * Cron expression for scheduled update (default: daily at 2 AM)
     */
    private String cronExpression = "0 0 2 * * *";
}

