package com.graduate.novel.domain.recommendation;

import com.graduate.novel.domain.recommendation.evaluation.OfflineEvaluator;
import com.graduate.novel.domain.recommendation.metrics.MetricsService;
import com.graduate.novel.domain.recommendation.metrics.RecommendationMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Recommendation System Evaluation
 * Mục đích: Kiểm chứng hiệu quả của các thuật toán gợi ý
 * Sử dụng cho báo cáo: 4.x. Thực nghiệm đánh giá độ chính xác gợi ý
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Recommendation System Evaluation Tests")
public class RecommendationEvaluationTest {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private OfflineEvaluator offlineEvaluator;

    @Test
    @DisplayName("Test single user metrics calculation")
    void testSingleUserMetrics() {
        // Given: A user with some interactions
        Long testUserId = 1L; // Adjust based on your test data
        int k = 10;

        // When: Calculate metrics for this user
        RecommendationMetrics metrics = metricsService.calculateMetrics(testUserId, k);

        // Then: Verify metrics are calculated (may be 0 if user has insufficient data)
        assertNotNull(metrics, "Metrics should not be null");
        assertNotNull(metrics.getK(), "K value should be set");
        assertEquals(k, metrics.getK());

        // Log results for report
        System.out.println("\n========== Single User Metrics (User ID: " + testUserId + ") ==========");
        System.out.println("K = " + k);
        System.out.println("Precision@K: " + formatMetric(metrics.getPrecisionAtK()));
        System.out.println("Recall@K: " + formatMetric(metrics.getRecallAtK()));
        System.out.println("F1@K: " + formatMetric(metrics.getF1ScoreAtK()));
        System.out.println("NDCG@K: " + formatMetric(metrics.getNdcgAtK()));
        System.out.println("MRR: " + formatMetric(metrics.getMrr()));
        System.out.println("Total Recommendations: " + metrics.getTotalRecommendations());
    }

    @Test
    @DisplayName("Test aggregate metrics for multiple users")
    void testAggregateMetrics() {
        // Given: Multiple users
        List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        int k = 10;

        // When: Calculate aggregate metrics
        RecommendationMetrics metrics = metricsService.calculateAggregateMetrics(userIds, k);

        // Then: Verify
        assertNotNull(metrics);

        System.out.println("\n========== Aggregate Metrics (Users: " + userIds.size() + ") ==========");
        System.out.println("K = " + k);
        System.out.println("Precision@K: " + formatMetric(metrics.getPrecisionAtK()));
        System.out.println("Recall@K: " + formatMetric(metrics.getRecallAtK()));
        System.out.println("F1@K: " + formatMetric(metrics.getF1ScoreAtK()));
        System.out.println("MAP@K: " + formatMetric(metrics.getMapAtK()));
        System.out.println("NDCG@K: " + formatMetric(metrics.getNdcgAtK()));
        System.out.println("MRR: " + formatMetric(metrics.getMrr()));
        System.out.println("Coverage: " + formatMetric(metrics.getCoverage()));
        System.out.println("Diversity: " + formatMetric(metrics.getDiversity()));
    }

    @Test
    @DisplayName("Test full offline evaluation with multiple K values")
    void testFullOfflineEvaluation() {
        // Given: Maximum users to evaluate
        int maxUsers = 20;

        // When: Run full evaluation
        OfflineEvaluator.EvaluationReport report = offlineEvaluator.runFullEvaluation(maxUsers);

        // Then: Verify report structure
        assertNotNull(report);
        assertNotNull(report.getEvaluationTime());
        assertNotNull(report.getKMetrics());
        assertFalse(report.getKMetrics().isEmpty(), "Should have metrics for different K values");

        // Print report for documentation
        System.out.println("\n" + report.getSummary());
    }

    @Test
    @DisplayName("Test metrics with different K values")
    void testMetricsWithDifferentKValues() {
        Long testUserId = 1L;
        int[] kValues = {5, 10, 20, 50};

        System.out.println("\n========== Metrics at Different K Values (User ID: " + testUserId + ") ==========");
        System.out.printf("%-6s | %-12s | %-12s | %-12s | %-12s%n",
            "K", "Precision", "Recall", "F1", "NDCG");
        System.out.println("-".repeat(65));

        for (int k : kValues) {
            RecommendationMetrics metrics = metricsService.calculateMetrics(testUserId, k);

            System.out.printf("%-6d | %-12s | %-12s | %-12s | %-12s%n",
                k,
                formatMetric(metrics.getPrecisionAtK()),
                formatMetric(metrics.getRecallAtK()),
                formatMetric(metrics.getF1ScoreAtK()),
                formatMetric(metrics.getNdcgAtK())
            );

            // Verify metrics are within valid range [0, 1]
            if (metrics.getPrecisionAtK() != null) {
                assertTrue(metrics.getPrecisionAtK() >= 0 && metrics.getPrecisionAtK() <= 1,
                    "Precision should be between 0 and 1");
            }
            if (metrics.getRecallAtK() != null) {
                assertTrue(metrics.getRecallAtK() >= 0 && metrics.getRecallAtK() <= 1,
                    "Recall should be between 0 and 1");
            }
        }
    }

    @Test
    @DisplayName("Test evaluation system performance")
    void testEvaluationSystemPerformance() {
        int k = 10;
        int maxUsers = 50;

        long startTime = System.currentTimeMillis();
        RecommendationMetrics metrics = metricsService.evaluateSystem(k, maxUsers);
        long endTime = System.currentTimeMillis();

        assertNotNull(metrics, "Metrics should not be null");

        long duration = endTime - startTime;

        System.out.println("\n========== System Evaluation Performance ==========");
        System.out.println("Users evaluated: " + maxUsers);
        System.out.println("K value: " + k);
        System.out.println("Execution time: " + duration + "ms");
        System.out.println("Avg time per user: " + (duration / maxUsers) + "ms");

        // Performance assertion: should complete within reasonable time
        // Allow 2 seconds per user as upper limit
        assertTrue(duration < maxUsers * 2000,
            "Evaluation should complete within reasonable time");
    }

    @Test
    @DisplayName("Test coverage and diversity metrics")
    void testCoverageAndDiversity() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        int k = 20;

        RecommendationMetrics metrics = metricsService.calculateAggregateMetrics(userIds, k);

        System.out.println("\n========== Coverage and Diversity Analysis ==========");
        System.out.println("Users: " + userIds.size());
        System.out.println("K: " + k);
        System.out.println("Coverage (unique items): " + formatMetric(metrics.getCoverage()));
        System.out.println("Diversity: " + formatMetric(metrics.getDiversity()));

        // Coverage should increase with more users
        // Diversity should be > 0 if recommendations have different genres
        if (metrics.getCoverage() != null) {
            assertTrue(metrics.getCoverage() >= 0, "Coverage should be non-negative");
        }
        if (metrics.getDiversity() != null) {
            assertTrue(metrics.getDiversity() >= 0 && metrics.getDiversity() <= 1,
                "Diversity should be between 0 and 1");
        }
    }

    /**
     * Format metric value for display
     */
    private String formatMetric(Double value) {
        if (value == null) return "N/A";
        return String.format("%.4f", value);
    }
}
