package com.graduate.novel.domain.recommendation;

import com.graduate.novel.domain.favorite.FavoriteRepository;
import com.graduate.novel.domain.history.ReadingHistoryRepository;
import com.graduate.novel.domain.rating.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final ReadingHistoryRepository historyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;

    /**
     * Analyze user's genre preferences based on reading history, ratings, and favorites
     */
    @Transactional(readOnly = true)
    public List<GenrePreference> analyzeGenrePreferences(Long userId) {
        log.info("Analyzing genre preferences for user: {}", userId);

        Map<Long, GenrePreferenceBuilder> genreScores = new HashMap<>();

        // 1. From Reading History (weight: 1.0)
        var readingHistory = historyRepository.findByUserIdOrderByLastReadAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        );

        readingHistory.getContent().forEach(history -> {
            if (history.getStory() != null && history.getStory().getGenres() != null) {
                history.getStory().getGenres().forEach(genre -> {
                    genreScores.computeIfAbsent(genre.getId(),
                        id -> new GenrePreferenceBuilder(id, genre.getName()))
                        .addScore(1.0);
                });
            }
        });

        // 2. From Ratings (weight: 2.0 for high ratings, -0.5 for low ratings)
        var ratings = ratingRepository.findByUserId(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        );

        ratings.getContent().forEach(rating -> {
            if (rating.getStory() != null && rating.getStory().getGenres() != null) {
                double weight = rating.getRating() >= 4 ? 2.0 :
                               rating.getRating() == 3 ? 0.5 : -0.5;

                rating.getStory().getGenres().forEach(genre -> {
                    genreScores.computeIfAbsent(genre.getId(),
                        id -> new GenrePreferenceBuilder(id, genre.getName()))
                        .addScore(weight);
                });
            }
        });

        // 3. From Favorites (weight: 3.0 - strongest signal)
        var favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        );

        favorites.getContent().forEach(favorite -> {
            if (favorite.getStory() != null && favorite.getStory().getGenres() != null) {
                favorite.getStory().getGenres().forEach(genre -> {
                    genreScores.computeIfAbsent(genre.getId(),
                        id -> new GenrePreferenceBuilder(id, genre.getName()))
                        .addScore(3.0);
                });
            }
        });

        // Build and sort preferences
        List<GenrePreference> preferences = genreScores.values().stream()
            .map(GenrePreferenceBuilder::build)
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());

        log.info("Found {} genre preferences for user {}", preferences.size(), userId);
        return preferences;
    }

    /**
     * Get list of story IDs the user has already interacted with
     */
    @Transactional(readOnly = true)
    public Set<Long> getUserInteractedStoryIds(Long userId) {
        Set<Long> storyIds = new HashSet<>();

        // From reading history
        var readHistory = historyRepository.findByUserIdOrderByLastReadAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 200)
        );
        readHistory.getContent().forEach(h -> {
            if (h.getStory() != null) storyIds.add(h.getStory().getId());
        });

        // From ratings
        var ratings = ratingRepository.findByUserId(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 200)
        );
        ratings.getContent().forEach(r -> {
            if (r.getStory() != null) storyIds.add(r.getStory().getId());
        });

        // From favorites
        var favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 200)
        );
        favorites.getContent().forEach(f -> {
            if (f.getStory() != null) storyIds.add(f.getStory().getId());
        });

        log.info("User {} has interacted with {} stories", userId, storyIds.size());
        return storyIds;
    }

    /**
     * Find similar users based on common story interactions
     */
    @Transactional(readOnly = true)
    public List<UserSimilarity> findSimilarUsers(Long userId, int limit) {
        log.info("Finding similar users for user: {}", userId);

        // Get current user's rated stories
        var currentUserRatings = ratingRepository.findByUserId(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();

        if (currentUserRatings.isEmpty()) {
            log.info("User {} has no ratings, cannot find similar users", userId);
            return Collections.emptyList();
        }

        Map<Long, Integer> currentUserStoryRatings = currentUserRatings.stream()
            .filter(r -> r.getStory() != null)
            .collect(Collectors.toMap(
                r -> r.getStory().getId(),
                r -> r.getRating()
            ));

        Set<Long> currentUserStoryIds = currentUserStoryRatings.keySet();

        // Find other users who rated the same stories
        Map<Long, List<Integer>> otherUsersRatings = new HashMap<>();

        for (Long storyId : currentUserStoryIds) {
            var storyRatings = ratingRepository.findByStoryId(
                storyId,
                org.springframework.data.domain.PageRequest.of(0, 100)
            ).getContent();

            storyRatings.forEach(rating -> {
                Long otherUserId = rating.getUser().getId();
                if (!otherUserId.equals(userId)) {
                    otherUsersRatings.computeIfAbsent(otherUserId, k -> new ArrayList<>())
                        .add(rating.getRating());
                }
            });
        }

        // Calculate similarity scores using Jaccard similarity
        List<UserSimilarity> similarities = otherUsersRatings.entrySet().stream()
            .map(entry -> {
                Long otherUserId = entry.getKey();
                int commonStories = entry.getValue().size();
                double similarity = (double) commonStories / currentUserStoryIds.size();

                return UserSimilarity.builder()
                    .userId(otherUserId)
                    .similarityScore(similarity)
                    .build();
            })
            .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
            .limit(limit)
            .collect(Collectors.toList());

        log.info("Found {} similar users for user {}", similarities.size(), userId);
        return similarities;
    }

    /**
     * Helper class to build genre preferences
     */
    private static class GenrePreferenceBuilder {
        private final Long genreId;
        private final String genreName;
        private double totalScore = 0.0;
        private int count = 0;

        public GenrePreferenceBuilder(Long genreId, String genreName) {
            this.genreId = genreId;
            this.genreName = genreName;
        }

        public void addScore(double score) {
            this.totalScore += score;
            this.count++;
        }

        public GenrePreference build() {
            return GenrePreference.builder()
                .genreId(genreId)
                .genreName(genreName)
                .score(totalScore)
                .interactionCount(count)
                .build();
        }
    }
}

