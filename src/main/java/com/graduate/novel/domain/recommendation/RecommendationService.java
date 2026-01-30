package com.graduate.novel.domain.recommendation;

import com.graduate.novel.ai.service.SemanticSearchService;
import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.rating.RatingRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserPreferenceService userPreferenceService;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final SemanticSearchService semanticSearchService;
    private final StoryMapper storyMapper;

    /**
     * Get hybrid recommendations - combines multiple algorithms
     */
    @Transactional(readOnly = true)
    public RecommendationDto getHybridRecommendations(Long userId, int limit) {
        log.info("Generating hybrid recommendations for user: {}", userId);

        // Get stories to exclude (already interacted)
        Set<Long> excludeStoryIds = userPreferenceService.getUserInteractedStoryIds(userId);

        return getHybridRecommendationsWithExclusions(userId, limit, excludeStoryIds);
    }

    /**
     * Get hybrid recommendations with custom exclusion list (for evaluation)
     */
    @Transactional(readOnly = true)
    public RecommendationDto getHybridRecommendationsWithExclusions(Long userId, int limit, Set<Long> excludeStoryIds) {
        log.info("Generating hybrid recommendations for user {} with {} exclusions", userId, excludeStoryIds.size());

        // Combine multiple recommendation sources with weights
        Map<Long, Double> storyScores = new HashMap<>();

        // 1. Content-based (40% weight) - based on genre preferences
        try {
            List<Story> contentBased = getContentBasedRecommendationsInternal(userId, limit * 2, excludeStoryIds);
            for (int i = 0; i < contentBased.size(); i++) {
                Story story = contentBased.get(i);
                double score = (contentBased.size() - i) * 0.4; // Decaying score
                storyScores.merge(story.getId(), score, Double::sum);
            }
            log.info("Added {} content-based recommendations", contentBased.size());
        } catch (Exception e) {
            log.warn("Content-based recommendations failed: {}", e.getMessage());
        }

        // 2. Collaborative filtering (30% weight) - based on similar users
        try {
            List<Story> collaborative = getCollaborativeRecommendationsInternal(userId, limit * 2, excludeStoryIds);
            for (int i = 0; i < collaborative.size(); i++) {
                Story story = collaborative.get(i);
                double score = (collaborative.size() - i) * 0.3;
                storyScores.merge(story.getId(), score, Double::sum);
            }
            log.info("Added {} collaborative recommendations", collaborative.size());
        } catch (Exception e) {
            log.warn("Collaborative filtering failed: {}", e.getMessage());
        }

        // 3. Trending stories (20% weight) - popular recent stories
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            var trending = storyRepository.findTrendingStories(since, PageRequest.of(0, limit));
            for (int i = 0; i < trending.getContent().size(); i++) {
                Story story = trending.getContent().get(i);
                if (!excludeStoryIds.contains(story.getId())) {
                    double score = (trending.getContent().size() - i) * 0.2;
                    storyScores.merge(story.getId(), score, Double::sum);
                }
            }
            log.info("Added {} trending stories", trending.getContent().size());
        } catch (Exception e) {
            log.warn("Trending stories failed: {}", e.getMessage());
        }

        // 4. High-rated stories (10% weight) - fallback
        try {
            var highRated = storyRepository.findAll(
                PageRequest.of(0, limit,
                    org.springframework.data.domain.Sort.by("averageRating").descending()
                        .and(org.springframework.data.domain.Sort.by("totalRatings").descending()))
            );
            for (int i = 0; i < highRated.getContent().size(); i++) {
                Story story = highRated.getContent().get(i);
                if (!excludeStoryIds.contains(story.getId()) && story.getTotalRatings() != null && story.getTotalRatings() > 10) {
                    double score = (highRated.getContent().size() - i) * 0.1;
                    storyScores.merge(story.getId(), score, Double::sum);
                }
            }
            log.info("Added {} high-rated stories", highRated.getContent().size());
        } catch (Exception e) {
            log.warn("High-rated stories failed: {}", e.getMessage());
        }

        // Sort by combined score and get top N
        List<Long> topStoryIds = storyScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Fetch full story details
        List<Story> recommendedStories = storyRepository.findByIdInWithGenres(topStoryIds);

        // Sort by score order
        Map<Long, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < topStoryIds.size(); i++) {
            idToIndex.put(topStoryIds.get(i), i);
        }
        recommendedStories.sort(Comparator.comparingInt(s -> idToIndex.getOrDefault(s.getId(), Integer.MAX_VALUE)));

        List<StoryDto> storyDtos = recommendedStories.stream()
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        log.info("Generated {} hybrid recommendations for user {}", storyDtos.size(), userId);

        return RecommendationDto.builder()
            .stories(storyDtos)
            .type(RecommendationType.HYBRID)
            .totalCount(storyDtos.size())
            .explanation("Based on your reading history, preferences, and trending stories")
            .build();
    }

    /**
     * Content-based recommendations - based on user's genre preferences
     */
    @Transactional(readOnly = true)
    public RecommendationDto getContentBasedRecommendations(Long userId, int limit) {
        log.info("Generating content-based recommendations for user: {}", userId);

        Set<Long> excludeStoryIds = userPreferenceService.getUserInteractedStoryIds(userId);
        List<Story> stories = getContentBasedRecommendationsInternal(userId, limit, excludeStoryIds);

        List<StoryDto> storyDtos = stories.stream()
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        return RecommendationDto.builder()
            .stories(storyDtos)
            .type(RecommendationType.CONTENT_BASED)
            .totalCount(storyDtos.size())
            .explanation("Based on genres you enjoy")
            .build();
    }

    /**
     * Collaborative filtering recommendations - based on similar users
     */
    @Transactional(readOnly = true)
    public RecommendationDto getCollaborativeRecommendations(Long userId, int limit) {
        log.info("Generating collaborative recommendations for user: {}", userId);

        Set<Long> excludeStoryIds = userPreferenceService.getUserInteractedStoryIds(userId);
        List<Story> stories = getCollaborativeRecommendationsInternal(userId, limit, excludeStoryIds);

        List<StoryDto> storyDtos = stories.stream()
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        return RecommendationDto.builder()
            .stories(storyDtos)
            .type(RecommendationType.COLLABORATIVE)
            .totalCount(storyDtos.size())
            .explanation("Based on users with similar tastes")
            .build();
    }

    /**
     * Get similar stories based on a specific story (for "Similar Stories" section)
     */
    @Transactional(readOnly = true)
    public RecommendationDto getSimilarStories(Long storyId, Long userId, int limit) {
        log.info("Finding similar stories to story: {}", storyId);

        Story sourceStory = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        Set<Long> excludeStoryIds = new HashSet<>();
        excludeStoryIds.add(storyId); // Exclude the source story itself

        if (userId != null) {
            try {
                excludeStoryIds.addAll(userPreferenceService.getUserInteractedStoryIds(userId));
            } catch (Exception e) {
                log.warn("Failed to get user interacted stories: {}", e.getMessage());
            }
        }

        List<Story> similarStories = new ArrayList<>();

        // 1. Try semantic similarity first (if embeddings exist)
        if (sourceStory.getEmbedding() != null && !sourceStory.getEmbedding().isEmpty()
            && !sourceStory.getEmbedding().equals("") && !sourceStory.getEmbedding().equals("[]")) {
            try {
                log.debug("Attempting semantic similarity search for story: {}", storyId);
                List<Long> similarIds = storyRepository.findStoryIdsBySimilarity(
                    sourceStory.getEmbedding(),
                    limit * 2
                );

                if (similarIds != null && !similarIds.isEmpty()) {
                    similarIds.removeIf(excludeStoryIds::contains);
                    List<Story> semanticSimilar = storyRepository.findByIdInWithGenres(
                        similarIds.stream().limit(limit).collect(Collectors.toList())
                    );
                    similarStories.addAll(semanticSimilar);
                    log.info("Found {} semantically similar stories", semanticSimilar.size());
                }
            } catch (Exception e) {
                log.warn("Semantic similarity search failed: {}", e.getMessage(), e);
            }
        } else {
            log.debug("No valid embedding for story {}, skipping semantic search", storyId);
        }

        // 2. Fallback to genre-based similarity
        if (similarStories.size() < limit && sourceStory.getGenres() != null && !sourceStory.getGenres().isEmpty()) {
            try {
                Long primaryGenreId = sourceStory.getGenres().iterator().next().getId();
                log.debug("Using genre-based similarity with genre ID: {}", primaryGenreId);

                var genreBased = storyRepository.findByGenreId(
                    primaryGenreId,
                    PageRequest.of(0, limit * 2)
                );

                List<Story> filtered = genreBased.getContent().stream()
                    .filter(s -> !excludeStoryIds.contains(s.getId()))
                    .filter(s -> !similarStories.contains(s))
                    .limit(limit - similarStories.size())
                    .collect(Collectors.toList());

                similarStories.addAll(filtered);
                log.info("Added {} genre-based similar stories", filtered.size());
            } catch (Exception e) {
                log.warn("Genre-based similarity failed: {}", e.getMessage(), e);
            }
        }

        // 3. Final fallback: get trending stories if still empty
        if (similarStories.isEmpty()) {
            log.warn("No similar stories found using embeddings or genres, using trending stories as fallback");
            try {
                LocalDateTime since = LocalDateTime.now().minusDays(30);
                var trending = storyRepository.findTrendingStories(since, PageRequest.of(0, limit));

                List<Story> filtered = trending.getContent().stream()
                    .filter(s -> !excludeStoryIds.contains(s.getId()))
                    .limit(limit)
                    .collect(Collectors.toList());

                similarStories.addAll(filtered);
                log.info("Added {} trending stories as fallback", filtered.size());
            } catch (Exception e) {
                log.error("Failed to get trending stories fallback: {}", e.getMessage(), e);
            }
        }

        List<StoryDto> storyDtos = similarStories.stream()
            .map(storyMapper::toDto)
            .collect(Collectors.toList());

        String explanation = similarStories.isEmpty()
            ? "No similar stories found"
            : "Similar to: " + (sourceStory.getTitle() != null ? sourceStory.getTitle() : sourceStory.getRawTitle());

        return RecommendationDto.builder()
            .stories(storyDtos)
            .type(RecommendationType.SEMANTIC)
            .totalCount(storyDtos.size())
            .explanation(explanation)
            .build();
    }

    // ========== Internal Helper Methods ==========

    private List<Story> getContentBasedRecommendationsInternal(Long userId, int limit, Set<Long> excludeIds) {
        // Get user's genre preferences
        List<GenrePreference> preferences = userPreferenceService.analyzeGenrePreferences(userId);

        if (preferences.isEmpty()) {
            log.info("User {} has no genre preferences, returning trending stories", userId);
            return storyRepository.findTrendingStories(
                LocalDateTime.now().minusDays(30),
                PageRequest.of(0, limit)
            ).getContent();
        }

        // Get stories from preferred genres
        List<Story> recommendations = new ArrayList<>();

        for (GenrePreference pref : preferences) {
            if (recommendations.size() >= limit) break;

            int needed = limit - recommendations.size();
            var genreStories = storyRepository.findByGenreId(
                pref.getGenreId(),
                PageRequest.of(0, needed * 2)
            );

            List<Story> filtered = genreStories.getContent().stream()
                .filter(s -> !excludeIds.contains(s.getId()))
                .filter(s -> !recommendations.contains(s))
                .limit(needed)
                .collect(Collectors.toList());

            recommendations.addAll(filtered);
        }

        return recommendations;
    }

    private List<Story> getCollaborativeRecommendationsInternal(Long userId, int limit, Set<Long> excludeIds) {
        // Find similar users
        List<UserSimilarity> similarUsers = userPreferenceService.findSimilarUsers(userId, 10);

        if (similarUsers.isEmpty()) {
            log.info("No similar users found for user {}", userId);
            return Collections.emptyList();
        }

        // Get highly-rated stories from similar users
        Map<Long, Double> storyScores = new HashMap<>();

        for (UserSimilarity similar : similarUsers) {
            var ratings = ratingRepository.findByUserId(
                similar.getUserId(),
                PageRequest.of(0, 50)
            );

            ratings.getContent().forEach(rating -> {
                if (rating.getRating() >= 4 && rating.getStory() != null) {
                    Long storyId = rating.getStory().getId();
                    if (!excludeIds.contains(storyId)) {
                        double score = rating.getRating() * similar.getSimilarityScore();
                        storyScores.merge(storyId, score, Double::sum);
                    }
                }
            });
        }

        // Sort by score and get top stories
        List<Long> topStoryIds = storyScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return storyRepository.findByIdInWithGenres(topStoryIds);
    }
}

