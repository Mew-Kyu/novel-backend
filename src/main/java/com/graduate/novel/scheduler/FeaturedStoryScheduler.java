package com.graduate.novel.scheduler;

import com.graduate.novel.config.FeaturedStoryProperties;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to automatically update featured stories based on performance metrics
 * Runs daily to promote high-performing stories and demote old ones
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.featured-stories", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeaturedStoryScheduler {

    private final StoryRepository storyRepository;
    private final FeaturedStoryProperties properties;

    /**
     * Run daily at configured time to update featured stories
     * Default: 2 AM daily
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "${app.featured-stories.cron-expression:0 0 2 * * *}")
    @Transactional
    @CacheEvict(value = "featuredStories", allEntries = true)
    public void updateFeaturedStories() {
        log.info("Starting scheduled featured stories update");

        try {
            // Step 1: Reset all current featured stories
            resetAllFeaturedStories();

            // Step 2: Find and promote top performing stories
            List<Story> topStories = findTopPerformingStories();

            // Step 3: Mark them as featured
            int promoted = promoteStories(topStories);

            log.info("Featured stories update completed. Promoted {} stories", promoted);
        } catch (Exception e) {
            log.error("Error updating featured stories", e);
        }
    }

    /**
     * Reset all stories to not featured
     */
    private void resetAllFeaturedStories() {
        List<Story> currentFeatured = storyRepository.findAll()
                .stream()
                .filter(Story::getFeatured)
                .toList();

        currentFeatured.forEach(story -> story.setFeatured(false));
        storyRepository.saveAll(currentFeatured);

        log.info("Reset {} previously featured stories", currentFeatured.size());
    }

    /**
     * Find top performing stories based on criteria
     */
    private List<Story> findTopPerformingStories() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(properties.getMinDaysSinceUpdate());

        return storyRepository.findAll()
                .stream()
                .filter(story -> isEligibleForFeatured(story, cutoffDate))
                .sorted((s1, s2) -> {
                    // Calculate score for each story
                    double score1 = calculateScore(s1);
                    double score2 = calculateScore(s2);
                    return Double.compare(score2, score1); // Descending order
                })
                .limit(properties.getMaxCount())
                .toList();
    }

    /**
     * Check if story is eligible for featured status
     */
    private boolean isEligibleForFeatured(Story story, LocalDateTime cutoffDate) {
        // Must have minimum view count
        if (story.getViewCount() < properties.getMinViewCount()) {
            return false;
        }

        // Must have good rating (if rated)
        if (story.getAverageRating() != null && story.getAverageRating() < properties.getMinRating()) {
            return false;
        }

        // Must be updated recently
        if (story.getUpdatedAt() == null || story.getUpdatedAt().isBefore(cutoffDate)) {
            return false;
        }

        return true;
    }

    /**
     * Calculate score for story based on multiple factors
     * Higher score = better candidate for featured
     */
    private double calculateScore(Story story) {
        double score = 0.0;

        // View count contribution (normalized)
        score += (story.getViewCount() / 1000.0) * properties.getViewCountWeight();

        // Rating contribution
        if (story.getAverageRating() != null && story.getTotalRatings() > 0) {
            // Weight rating by number of ratings (more ratings = more reliable)
            double ratingScore = story.getAverageRating() * Math.log10(story.getTotalRatings() + 1);
            score += ratingScore * properties.getRatingWeight();
        }

        // Recency contribution
        if (story.getUpdatedAt() != null) {
            long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS.between(
                    story.getUpdatedAt(), LocalDateTime.now());
            // Newer = higher score (exponential decay)
            double recencyScore = Math.exp(-daysSinceUpdate / 7.0) * 10;
            score += recencyScore * properties.getRecencyWeight();
        }

        return score;
    }

    /**
     * Promote stories to featured status
     */
    private int promoteStories(List<Story> stories) {
        stories.forEach(story -> {
            story.setFeatured(true);
            log.debug("Promoted story '{}' (ID: {}) - Views: {}, Rating: {}, Score: {}",
                    story.getTitle(), story.getId(), story.getViewCount(),
                    story.getAverageRating(), calculateScore(story));
        });

        storyRepository.saveAll(stories);
        return stories.size();
    }

    /**
     * Manual trigger for testing/admin purposes
     */
    @Transactional
    @CacheEvict(value = "featuredStories", allEntries = true)
    public void manualUpdateFeaturedStories() {
        log.info("Manual trigger: updating featured stories");
        updateFeaturedStories();
    }
}

