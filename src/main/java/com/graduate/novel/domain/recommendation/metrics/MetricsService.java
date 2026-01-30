package com.graduate.novel.domain.recommendation.metrics;

import com.graduate.novel.domain.favorite.FavoriteRepository;
import com.graduate.novel.domain.rating.RatingRepository;
import com.graduate.novel.domain.recommendation.RecommendationService;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating recommendation quality metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final RecommendationService recommendationService;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    /**
     * Calculate all metrics for a user's recommendations
     */
    @Transactional(readOnly = true)
    public RecommendationMetrics calculateMetrics(Long userId, int k) {
        log.info("Calculating metrics for user {} with K={}", userId, k);

        // Get ALL relevant items (items user actually liked)
        Set<Long> allRelevantItems = getRelevantItems(userId);

        if (allRelevantItems.isEmpty()) {
            log.warn("User {} has no relevant items, cannot calculate metrics", userId);
            return RecommendationMetrics.builder()
                .k(k)
                .totalUsers(1)
                .build();
        }

        // TRAIN-TEST SPLIT: Split relevant items into 80% training, 20% test
        List<Long> relevantList = new ArrayList<>(allRelevantItems);
        Collections.shuffle(relevantList, new Random(userId)); // Deterministic shuffle based on userId

        int splitIndex = (int) (relevantList.size() * 0.8);
        Set<Long> trainingSet = new HashSet<>(relevantList.subList(0, splitIndex));
        Set<Long> testSet = new HashSet<>(relevantList.subList(splitIndex, relevantList.size()));

        if (testSet.isEmpty()) {
            log.warn("User {} has insufficient relevant items for train-test split (total: {})",
                userId, allRelevantItems.size());
            return RecommendationMetrics.builder()
                .k(k)
                .totalUsers(1)
                .build();
        }

        log.info("User {} - Training set: {}, Test set: {}", userId, trainingSet.size(), testSet.size());

        // Get recommendations - exclude only training set (simulate hiding test interactions)
        var recommendations = recommendationService.getHybridRecommendationsWithExclusions(userId, k, trainingSet);
        List<Long> recommendedIds = recommendations.getStories().stream()
            .map(StoryDto::id)
            .collect(Collectors.toList());

        log.info("User {} - Recommended {} items", userId, recommendedIds.size());

        // Calculate metrics - evaluate against test set
        double precision = calculatePrecisionAtK(recommendedIds, testSet, k);
        double recall = calculateRecallAtK(recommendedIds, testSet, k);
        double f1Score = calculateF1Score(precision, recall);
        double map = calculateMAP(recommendedIds, testSet, k);
        double ndcg = calculateNDCG(recommendedIds, testSet, k);
        double mrr = calculateMRR(recommendedIds, testSet);

        return RecommendationMetrics.builder()
            .precisionAtK(precision)
            .recallAtK(recall)
            .f1ScoreAtK(f1Score)
            .mapAtK(map)
            .ndcgAtK(ndcg)
            .mrr(mrr)
            .k(k)
            .totalUsers(1)
            .totalRecommendations(recommendedIds.size())
            .build();
    }

    /**
     * Calculate metrics across multiple users (offline evaluation)
     */
    @Transactional(readOnly = true)
    public RecommendationMetrics calculateAggregateMetrics(List<Long> userIds, int k) {
        log.info("Calculating aggregate metrics for {} users with K={}", userIds.size(), k);

        List<RecommendationMetrics> allMetrics = new ArrayList<>();
        Map<Long, RecommendationMetrics.UserMetrics> perUserMetrics = new HashMap<>();

        for (Long userId : userIds) {
            try {
                RecommendationMetrics userMetric = calculateMetrics(userId, k);
                allMetrics.add(userMetric);

                // Store per-user metrics
                perUserMetrics.put(userId, RecommendationMetrics.UserMetrics.builder()
                    .userId(userId)
                    .precision(userMetric.getPrecisionAtK())
                    .recall(userMetric.getRecallAtK())
                    .ndcg(userMetric.getNdcgAtK())
                    .build());
            } catch (Exception e) {
                log.warn("Failed to calculate metrics for user {}: {}", userId, e.getMessage());
            }
        }

        if (allMetrics.isEmpty()) {
            return RecommendationMetrics.builder()
                .k(k)
                .totalUsers(userIds.size())
                .build();
        }

        // Average all metrics
        return RecommendationMetrics.builder()
            .precisionAtK(average(allMetrics, RecommendationMetrics::getPrecisionAtK))
            .recallAtK(average(allMetrics, RecommendationMetrics::getRecallAtK))
            .f1ScoreAtK(average(allMetrics, RecommendationMetrics::getF1ScoreAtK))
            .mapAtK(average(allMetrics, RecommendationMetrics::getMapAtK))
            .ndcgAtK(average(allMetrics, RecommendationMetrics::getNdcgAtK))
            .mrr(average(allMetrics, RecommendationMetrics::getMrr))
            .coverage(calculateCoverage(userIds, k))
            .diversity(calculateDiversity(userIds, k))
            .k(k)
            .totalUsers(userIds.size())
            .totalRecommendations(allMetrics.stream()
                .mapToInt(m -> m.getTotalRecommendations() != null ? m.getTotalRecommendations() : 0)
                .sum())
            .perUserMetrics(perUserMetrics)
            .build();
    }

    /**
     * Calculate metrics for all active users
     */
    @Transactional(readOnly = true)
    public RecommendationMetrics evaluateSystem(int k, int maxUsers) {
        log.info("Evaluating recommendation system with K={}, maxUsers={}", k, maxUsers);

        List<User> users = userRepository.findAll(PageRequest.of(0, maxUsers)).getContent();
        List<Long> userIds = users.stream()
            .map(User::getId)
            .collect(Collectors.toList());

        return calculateAggregateMetrics(userIds, k);
    }

    // ========== Metric Calculation Methods ==========

    /**
     * Precision@K = (# relevant items in top K) / K
     */
    private double calculatePrecisionAtK(List<Long> recommended, Set<Long> relevant, int k) {
        if (recommended.isEmpty()) return 0.0;

        long relevantInTopK = recommended.stream()
            .limit(k)
            .filter(relevant::contains)
            .count();

        return (double) relevantInTopK / Math.min(k, recommended.size());
    }

    /**
     * Recall@K = (# relevant items in top K) / (total # relevant items)
     */
    private double calculateRecallAtK(List<Long> recommended, Set<Long> relevant, int k) {
        if (relevant.isEmpty()) return 0.0;

        long relevantInTopK = recommended.stream()
            .limit(k)
            .filter(relevant::contains)
            .count();

        return (double) relevantInTopK / relevant.size();
    }

    /**
     * F1-Score = 2 * (Precision * Recall) / (Precision + Recall)
     */
    private double calculateF1Score(double precision, double recall) {
        if (precision + recall == 0) return 0.0;
        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * MAP@K (Mean Average Precision)
     * Average precision across all relevant items in top K
     */
    private double calculateMAP(List<Long> recommended, Set<Long> relevant, int k) {
        if (relevant.isEmpty()) return 0.0;

        double sum = 0.0;
        int relevantCount = 0;

        for (int i = 0; i < Math.min(k, recommended.size()); i++) {
            if (relevant.contains(recommended.get(i))) {
                relevantCount++;
                // Precision at position i+1
                double precisionAtI = (double) relevantCount / (i + 1);
                sum += precisionAtI;
            }
        }

        return sum / Math.min(relevant.size(), k);
    }

    /**
     * NDCG@K (Normalized Discounted Cumulative Gain)
     * Measures ranking quality with position discount
     */
    private double calculateNDCG(List<Long> recommended, Set<Long> relevant, int k) {
        if (relevant.isEmpty()) return 0.0;

        // DCG (Discounted Cumulative Gain)
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, recommended.size()); i++) {
            if (relevant.contains(recommended.get(i))) {
                // Using binary relevance (1 if relevant, 0 otherwise)
                // DCG formula: sum(rel_i / log2(i+2))
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2));
            }
        }

        // IDCG (Ideal DCG) - all relevant items at the top
        double idcg = 0.0;
        for (int i = 0; i < Math.min(k, relevant.size()); i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }

        return idcg > 0 ? dcg / idcg : 0.0;
    }

    /**
     * MRR (Mean Reciprocal Rank)
     * 1 / (position of first relevant item)
     */
    private double calculateMRR(List<Long> recommended, Set<Long> relevant) {
        for (int i = 0; i < recommended.size(); i++) {
            if (relevant.contains(recommended.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /**
     * Coverage - What proportion of all items can be recommended
     */
    private double calculateCoverage(List<Long> userIds, int k) {
        Set<Long> allRecommendedItems = new HashSet<>();

        for (Long userId : userIds) {
            try {
                // Get relevant items and split for training
                Set<Long> allRelevantItems = getRelevantItems(userId);
                if (allRelevantItems.size() < 2) continue;

                List<Long> relevantList = new ArrayList<>(allRelevantItems);
                Collections.shuffle(relevantList, new Random(userId));
                int splitIndex = (int) (relevantList.size() * 0.8);
                Set<Long> trainingSet = new HashSet<>(relevantList.subList(0, splitIndex));

                var recommendations = recommendationService.getHybridRecommendationsWithExclusions(userId, k, trainingSet);
                recommendations.getStories().forEach(story ->
                    allRecommendedItems.add(story.id())
                );
            } catch (Exception e) {
                log.warn("Failed to get recommendations for coverage calculation: {}", e.getMessage());
            }
        }

        // This is a simplified version - ideally you'd divide by total items in catalog
        // For now, return the count (can be normalized later with total story count)
        return allRecommendedItems.size();
    }

    /**
     * Diversity - Average pairwise dissimilarity of recommendations
     * (Simplified version - counts unique genres)
     */
    private double calculateDiversity(List<Long> userIds, int k) {
        List<Double> diversityScores = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                // Get relevant items and split for training
                Set<Long> allRelevantItems = getRelevantItems(userId);
                if (allRelevantItems.size() < 2) continue;

                List<Long> relevantList = new ArrayList<>(allRelevantItems);
                Collections.shuffle(relevantList, new Random(userId));
                int splitIndex = (int) (relevantList.size() * 0.8);
                Set<Long> trainingSet = new HashSet<>(relevantList.subList(0, splitIndex));

                var recommendations = recommendationService.getHybridRecommendationsWithExclusions(userId, k, trainingSet);

                // Count unique genres in recommendations
                Set<String> uniqueGenres = new HashSet<>();
                recommendations.getStories().forEach(story -> {
                    if (story.genres() != null) {
                        story.genres().forEach(genre ->
                            uniqueGenres.add(genre.name())
                        );
                    }
                });

                // Diversity = unique genres / total recommendations
                double diversity = recommendations.getStories().isEmpty() ? 0.0 :
                    (double) uniqueGenres.size() / recommendations.getStories().size();
                diversityScores.add(diversity);
            } catch (Exception e) {
                log.warn("Failed to calculate diversity: {}", e.getMessage());
            }
        }

        return diversityScores.isEmpty() ? 0.0 :
            diversityScores.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    // ========== Helper Methods ==========

    /**
     * Get items that user actually liked (ground truth)
     * Based on high ratings (>=4) and favorites
     */
    private Set<Long> getRelevantItems(Long userId) {
        Set<Long> relevant = new HashSet<>();

        // From high ratings (4-5 stars)
        var ratings = ratingRepository.findByUserId(userId, PageRequest.of(0, 200));
        ratings.getContent().forEach(rating -> {
            if (rating.getRating() >= 4 && rating.getStory() != null) {
                relevant.add(rating.getStory().getId());
            }
        });

        // From favorites
        var favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200));
        favorites.getContent().forEach(favorite -> {
            if (favorite.getStory() != null) {
                relevant.add(favorite.getStory().getId());
            }
        });

        return relevant;
    }

    /**
     * Calculate average of a metric across all user metrics
     */
    private Double average(List<RecommendationMetrics> metrics,
                          java.util.function.Function<RecommendationMetrics, Double> getter) {
        return metrics.stream()
            .map(getter)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
}

