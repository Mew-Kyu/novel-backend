package com.graduate.novel.domain.recommendation.coldstart;

import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.history.ReadingHistoryRepository;
import com.graduate.novel.domain.rating.RatingRepository;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy for brand new users with no interaction history
 * Shows popular and trending stories
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewUserStrategy implements ColdStartStrategy {

    private final StoryRepository storyRepository;
    private final ReadingHistoryRepository historyRepository;
    private final RatingRepository ratingRepository;
    private final StoryMapper storyMapper;

    // Thresholds for considering a user as "new"
    private static final int MAX_INTERACTIONS = 3;

    @Override
    public List<StoryDto> getRecommendations(Long userId, int limit) {
        log.info("Applying new user strategy for user {}", userId);

        // Mix of trending and high-rated stories
        int trendingCount = (int) (limit * 0.7); // 70% trending
        int highRatedCount = limit - trendingCount; // 30% high-rated

        // Get trending stories (last 30 days)
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        var trending = storyRepository.findTrendingStories(since, PageRequest.of(0, trendingCount));

        // Get high-rated stories
        var highRated = storyRepository.findAll(
            PageRequest.of(0, highRatedCount + 10, // Get extra for filtering
                org.springframework.data.domain.Sort.by("averageRating").descending()
                    .and(org.springframework.data.domain.Sort.by("totalRatings").descending()))
        );

        // Combine and deduplicate
        List<StoryDto> recommendations = trending.getContent().stream()
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        highRated.getContent().stream()
            .filter(story -> story.getTotalRatings() != null && story.getTotalRatings() > 10)
            .filter(story -> recommendations.stream().noneMatch(r -> r.id().equals(story.getId())))
            .limit(highRatedCount)
            .map(storyMapper::toDto)
            .forEach(recommendations::add);

        log.info("New user strategy generated {} recommendations", recommendations.size());
        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public boolean isApplicable(Long userId) {
        if (userId == null) return true; // Anonymous users

        // Check interaction count
        long historyCount = historyRepository.countByUserId(userId);
        long ratingCount = ratingRepository.countByUserId(userId);

        boolean applicable = (historyCount + ratingCount) <= MAX_INTERACTIONS;

        log.debug("New user strategy applicable for user {}: {} (history: {}, ratings: {})",
            userId, applicable, historyCount, ratingCount);

        return applicable;
    }

    @Override
    public String getName() {
        return "NewUserStrategy";
    }

    @Override
    public int getPriority() {
        return 10; // High priority for new users
    }
}

