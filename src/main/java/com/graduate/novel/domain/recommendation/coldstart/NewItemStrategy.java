package com.graduate.novel.domain.recommendation.coldstart;

import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Strategy for new stories with little to no user interactions
 * Uses content-based features and trending genres
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewItemStrategy implements ColdStartStrategy {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    // Stories created within this many days are considered "new"
    private static final int NEW_ITEM_DAYS = 14;

    @Override
    public List<StoryDto> getRecommendations(Long userId, int limit) {
        log.info("Applying new item exploration strategy");

        // Get recently added stories
        LocalDateTime since = LocalDateTime.now().minusDays(NEW_ITEM_DAYS);

        var newStories = storyRepository.findAll(
            PageRequest.of(0, limit * 2, Sort.by("createdAt").descending())
        );

        // Filter and prioritize by content features
        List<StoryDto> recommendations = newStories.getContent().stream()
            .filter(story -> story.getCreatedAt() != null && story.getCreatedAt().isAfter(since))
            // Prioritize stories with embeddings (better quality)
            .sorted((a, b) -> {
                boolean aHasEmb = a.getEmbedding() != null && !a.getEmbedding().isEmpty();
                boolean bHasEmb = b.getEmbedding() != null && !b.getEmbedding().isEmpty();
                if (aHasEmb && !bHasEmb) return -1;
                if (!aHasEmb && bHasEmb) return 1;
                return 0;
            })
            .limit(limit)
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        log.info("New item strategy generated {} recommendations", recommendations.size());
        return recommendations;
    }

    @Override
    public boolean isApplicable(Long userId) {
        // This strategy is supplementary - can be mixed with others
        // Check if there are new items to explore
        LocalDateTime since = LocalDateTime.now().minusDays(NEW_ITEM_DAYS);
        long newItemCount = storyRepository.count(); // Simplified check

        return newItemCount > 0;
    }

    @Override
    public String getName() {
        return "NewItemStrategy";
    }

    @Override
    public int getPriority() {
        return 5; // Medium priority
    }

    /**
     * Get new stories for exploration
     * Can be called independently to boost new content
     */
    public List<StoryDto> getNewStoriesForExploration(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(NEW_ITEM_DAYS);

        var stories = storyRepository.findAll(
            PageRequest.of(0, limit, Sort.by("createdAt").descending())
        );

        return stories.getContent().stream()
            .filter(story -> story.getCreatedAt() != null && story.getCreatedAt().isAfter(since))
            .map(storyMapper::toDto)
            .collect(Collectors.toList());
    }
}

