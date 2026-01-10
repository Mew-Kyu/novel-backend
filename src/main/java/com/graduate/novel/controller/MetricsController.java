package com.graduate.novel.controller;

import com.graduate.novel.domain.recommendation.evaluation.OfflineEvaluator;
import com.graduate.novel.domain.recommendation.metrics.MetricsService;
import com.graduate.novel.domain.recommendation.metrics.RecommendationMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendation Metrics", description = "APIs for evaluating recommendation system quality")
public class MetricsController {

    private final MetricsService metricsService;
    private final OfflineEvaluator offlineEvaluator;

    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get recommendation metrics for specific user",
        description = "Calculate Precision@K, Recall@K, NDCG@K, MAP@K, etc. for a user's recommendations",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<RecommendationMetrics> getUserMetrics(
        @PathVariable Long userId,
        @Parameter(description = "Number of top recommendations to evaluate (K)")
        @RequestParam(defaultValue = "10") int k,
        Authentication authentication
    ) {
        log.info("Calculating metrics for user {} with K={}", userId, k);
        RecommendationMetrics metrics = metricsService.calculateMetrics(userId, k);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/aggregate")
    @Operation(
        summary = "Get aggregate metrics across multiple users",
        description = "Calculate average metrics across specified users. Requires ADMIN or MODERATOR role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<RecommendationMetrics> getAggregateMetrics(
        @Parameter(description = "Comma-separated user IDs")
        @RequestParam String userIds,
        @Parameter(description = "Number of top recommendations to evaluate (K)")
        @RequestParam(defaultValue = "10") int k
    ) {
        log.info("Calculating aggregate metrics with K={}", k);

        // Parse user IDs
        String[] ids = userIds.split(",");
        java.util.List<Long> userIdList = new java.util.ArrayList<>();
        for (String id : ids) {
            try {
                userIdList.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID: {}", id);
            }
        }

        RecommendationMetrics metrics = metricsService.calculateAggregateMetrics(userIdList, k);
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/evaluate-system")
    @Operation(
        summary = "Run full system evaluation (offline)",
        description = "Evaluate recommendation system with multiple K values. This is a heavy operation. Requires ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<OfflineEvaluator.EvaluationReport> evaluateSystem(
        @Parameter(description = "Maximum number of users to evaluate")
        @RequestParam(defaultValue = "100") int maxUsers
    ) {
        log.info("Starting full system evaluation with maxUsers={}", maxUsers);
        OfflineEvaluator.EvaluationReport report = offlineEvaluator.runFullEvaluation(maxUsers);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/evaluate-system/summary")
    @Operation(
        summary = "Run evaluation and get text summary",
        description = "Get human-readable summary of system evaluation. Requires ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> evaluateSystemSummary(
        @Parameter(description = "Maximum number of users to evaluate")
        @RequestParam(defaultValue = "50") int maxUsers
    ) {
        log.info("Starting evaluation summary with maxUsers={}", maxUsers);
        OfflineEvaluator.EvaluationReport report = offlineEvaluator.runFullEvaluation(maxUsers);
        return ResponseEntity.ok(report.getSummary());
    }
}

