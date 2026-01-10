package com.graduate.novel.domain.recommendation.evaluation;

import com.graduate.novel.domain.recommendation.metrics.MetricsService;
import com.graduate.novel.domain.recommendation.metrics.RecommendationMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline evaluation framework for recommendation system
 * Supports A/B testing and performance tracking over time
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineEvaluator {

    private final MetricsService metricsService;

    /**
     * Run full offline evaluation with multiple K values
     */
    public EvaluationReport runFullEvaluation(int maxUsers) {
        log.info("Starting full offline evaluation with maxUsers={}", maxUsers);

        LocalDateTime startTime = LocalDateTime.now();
        List<KMetrics> kMetricsList = new ArrayList<>();

        // Evaluate for different K values
        int[] kValues = {5, 10, 20, 50};

        for (int k : kValues) {
            log.info("Evaluating with K={}", k);
            RecommendationMetrics metrics = metricsService.evaluateSystem(k, maxUsers);

            kMetricsList.add(KMetrics.builder()
                .k(k)
                .metrics(metrics)
                .build());

            log.info("K={} Results - P@K: {}, R@K: {}, NDCG@K: {}",
                k,
                String.format("%.4f", metrics.getPrecisionAtK()),
                String.format("%.4f", metrics.getRecallAtK()),
                String.format("%.4f", metrics.getNdcgAtK())
            );
        }

        LocalDateTime endTime = LocalDateTime.now();

        return EvaluationReport.builder()
            .evaluationTime(startTime)
            .completionTime(endTime)
            .totalUsers(maxUsers)
            .kMetrics(kMetricsList)
            .build();
    }

    /**
     * Compare two recommendation algorithms
     */
    public ComparisonReport compareAlgorithms(String algorithmA, String algorithmB,
                                             int k, int maxUsers) {
        log.info("Comparing {} vs {} with K={}, maxUsers={}",
            algorithmA, algorithmB, k, maxUsers);

        // This is a placeholder - in real implementation, you'd switch algorithms
        // For now, we'll just run evaluation twice to demonstrate the structure
        RecommendationMetrics metricsA = metricsService.evaluateSystem(k, maxUsers);

        return ComparisonReport.builder()
            .algorithmA(algorithmA)
            .algorithmB(algorithmB)
            .metricsA(metricsA)
            .k(k)
            .totalUsers(maxUsers)
            .build();
    }

    // ========== DTOs ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EvaluationReport {
        private LocalDateTime evaluationTime;
        private LocalDateTime completionTime;
        private Integer totalUsers;
        private List<KMetrics> kMetrics;

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Offline Evaluation Report\n");
            sb.append("=========================\n");
            sb.append("Evaluation Time: ").append(evaluationTime).append("\n");
            sb.append("Total Users: ").append(totalUsers).append("\n\n");

            for (KMetrics km : kMetrics) {
                sb.append(String.format("K=%d:\n", km.getK()));
                RecommendationMetrics m = km.getMetrics();
                sb.append(String.format("  Precision@K: %.4f\n", m.getPrecisionAtK()));
                sb.append(String.format("  Recall@K: %.4f\n", m.getRecallAtK()));
                sb.append(String.format("  F1@K: %.4f\n", m.getF1ScoreAtK()));
                sb.append(String.format("  MAP@K: %.4f\n", m.getMapAtK()));
                sb.append(String.format("  NDCG@K: %.4f\n", m.getNdcgAtK()));
                sb.append(String.format("  MRR: %.4f\n", m.getMrr()));
                sb.append(String.format("  Coverage: %.2f items\n", m.getCoverage()));
                sb.append(String.format("  Diversity: %.4f\n\n", m.getDiversity()));
            }

            return sb.toString();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class KMetrics {
        private Integer k;
        private RecommendationMetrics metrics;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ComparisonReport {
        private String algorithmA;
        private String algorithmB;
        private RecommendationMetrics metricsA;
        private RecommendationMetrics metricsB;
        private Integer k;
        private Integer totalUsers;

        public String getSummary() {
            return String.format(
                "Algorithm Comparison (K=%d, Users=%d)\n" +
                "%s: P@K=%.4f, R@K=%.4f, NDCG@K=%.4f\n" +
                "%s: P@K=%.4f, R@K=%.4f, NDCG@K=%.4f\n",
                k, totalUsers,
                algorithmA,
                metricsA != null ? metricsA.getPrecisionAtK() : 0.0,
                metricsA != null ? metricsA.getRecallAtK() : 0.0,
                metricsA != null ? metricsA.getNdcgAtK() : 0.0,
                algorithmB,
                metricsB != null ? metricsB.getPrecisionAtK() : 0.0,
                metricsB != null ? metricsB.getRecallAtK() : 0.0,
                metricsB != null ? metricsB.getNdcgAtK() : 0.0
            );
        }
    }
}

