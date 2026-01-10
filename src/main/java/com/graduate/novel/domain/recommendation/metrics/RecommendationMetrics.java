package com.graduate.novel.domain.recommendation.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationMetrics {

    private Double precisionAtK;
    private Double recallAtK;
    private Double f1ScoreAtK;
    private Double mapAtK;
    private Double ndcgAtK;
    private Double mrr;
    private Double coverage;
    private Double diversity;
    private Double serendipity;
    private Double novelty;
    private Integer k;
    private Integer totalUsers;
    private Integer totalRecommendations;
    private Map<Long, UserMetrics> perUserMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetrics {
        private Long userId;
        private Double precision;
        private Double recall;
        private Double ndcg;
        private Integer relevantItemsFound;
        private Integer totalRelevantItems;
    }
}

