package com.graduate.novel.controller;

import com.graduate.novel.domain.recommendation.RecommendationDto;
import com.graduate.novel.domain.recommendation.RecommendationService;
import com.graduate.novel.domain.recommendation.coldstart.ColdStartService;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendations", description = "Personalized story recommendation APIs using Collaborative Filtering and Content-based Filtering")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ColdStartService coldStartService;

    /**
     * Get personalized recommendations for current user - "Có thể bạn sẽ thích"
     * Uses hybrid approach combining collaborative filtering and content-based filtering
     */
    @GetMapping("/for-you")
    @Operation(
        summary = "Get personalized recommendations (Có thể bạn sẽ thích)",
        description = "Get personalized story recommendations using hybrid approach:\n\n" +
            "**Algorithm Breakdown:**\n" +
            "- 40% Content-based Filtering (based on your favorite genres)\n" +
            "- 30% Collaborative Filtering (based on users with similar tastes)\n" +
            "- 20% Trending stories (popular recent stories)\n" +
            "- 10% High-rated stories (fallback)\n\n" +
            "**Features:**\n" +
            "- Analyzes your reading history, ratings, and favorites\n" +
            "- Excludes stories you've already read\n" +
            "- Adapts to your preferences over time\n" +
            "- Perfect for homepage 'You May Like' section",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recommendations retrieved successfully",
            content = @Content(schema = @Schema(implementation = RecommendationDto.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - login required")
    })
    public ResponseEntity<RecommendationDto> getRecommendationsForYou(
        @AuthenticationPrincipal User currentUser,
        @Parameter(description = "Number of recommendations to return", example = "10")
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting personalized recommendations for user: {}", currentUser.getId());
        RecommendationDto recommendations = recommendationService.getHybridRecommendations(
            currentUser.getId(),
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get content-based recommendations only
     */
    @GetMapping("/content-based")
    @Operation(
        summary = "Get content-based recommendations",
        description = "Get recommendations based only on your genre preferences and reading patterns.\n\n" +
            "**Algorithm:** Content-based Filtering\n" +
            "- Analyzes genres from your reading history\n" +
            "- Weighted by ratings (high ratings = prefer genre, low ratings = avoid genre)\n" +
            "- Weighted by favorites (strongest signal)\n" +
            "- Returns stories from your preferred genres",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Content-based recommendations retrieved",
            content = @Content(schema = @Schema(implementation = RecommendationDto.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RecommendationDto> getContentBasedRecommendations(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting content-based recommendations for user: {}", currentUser.getId());
        RecommendationDto recommendations = recommendationService.getContentBasedRecommendations(
            currentUser.getId(),
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get collaborative filtering recommendations only
     */
    @GetMapping("/collaborative")
    @Operation(
        summary = "Get collaborative filtering recommendations",
        description = "Get recommendations based on users with similar tastes.\n\n" +
            "**Algorithm:** Collaborative Filtering (User-based)\n" +
            "- Finds users who rated similar stories as you\n" +
            "- Calculates similarity score using Jaccard similarity\n" +
            "- Recommends highly-rated stories from similar users\n" +
            "- Weighted by similarity score and rating value",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collaborative recommendations retrieved",
            content = @Content(schema = @Schema(implementation = RecommendationDto.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RecommendationDto> getCollaborativeRecommendations(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting collaborative recommendations for user: {}", currentUser.getId());
        RecommendationDto recommendations = recommendationService.getCollaborativeRecommendations(
            currentUser.getId(),
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get similar stories to a specific story
     */
    @GetMapping("/similar/{storyId}")
    @Operation(
        summary = "Get similar stories",
        description = "Get stories similar to a specific story.\n\n" +
            "**Algorithm:** Semantic Similarity + Genre-based\n" +
            "- Primary: Uses semantic embeddings (pgvector + Gemini AI) for content similarity\n" +
            "- Fallback: Genre-based similarity if embeddings not available\n" +
            "- Perfect for 'Similar Stories' or 'Readers also liked' sections\n\n" +
            "**Use Cases:**\n" +
            "- Story detail page recommendations\n" +
            "- 'More like this' sections\n" +
            "- Related content suggestions",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Similar stories retrieved",
            content = @Content(schema = @Schema(implementation = RecommendationDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Story not found")
    })
    public ResponseEntity<RecommendationDto> getSimilarStories(
        @Parameter(description = "Story ID to find similar stories for", required = true)
        @PathVariable Long storyId,
        @AuthenticationPrincipal User currentUser,
        @Parameter(description = "Number of similar stories to return", example = "5")
        @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("Getting similar stories to story: {} for user: {}", storyId,
            currentUser != null ? currentUser.getId() : "anonymous");

        Long userId = currentUser != null ? currentUser.getId() : null;
        RecommendationDto recommendations = recommendationService.getSimilarStories(
            storyId,
            userId,
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get similar stories (public access - for anonymous users)
     */
    @GetMapping("/similar/{storyId}/public")
    @Operation(
        summary = "Get similar stories (public)",
        description = "Get similar stories without authentication. Same algorithm as authenticated endpoint but doesn't exclude user's read stories."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Similar stories retrieved",
            content = @Content(schema = @Schema(implementation = RecommendationDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Story not found")
    })
    public ResponseEntity<RecommendationDto> getSimilarStoriesPublic(
        @PathVariable Long storyId,
        @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("Getting similar stories to story: {} (public)", storyId);
        RecommendationDto recommendations = recommendationService.getSimilarStories(
            storyId,
            null,
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get recommendations for new users (cold-start)
     */
    @GetMapping("/cold-start")
    @Operation(
        summary = "Get cold-start recommendations",
        description = "Get recommendations for new users with little to no interaction history.\n\n" +
            "**Features:**\n" +
            "- Automatically detects if user is in cold-start state\n" +
            "- Uses trending and popular stories for new users\n" +
            "- Can be combined with onboarding preferences\n" +
            "- Gradually transitions to personalized recommendations as user interacts",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<StoryDto>> getColdStartRecommendations(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting cold-start recommendations for user: {}", currentUser.getId());
        List<StoryDto> recommendations = coldStartService.getMixedRecommendations(
            currentUser.getId(),
            limit
        );
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Check if user is in cold-start state
     */
    @GetMapping("/cold-start/check")
    @Operation(
        summary = "Check if user is in cold-start state",
        description = "Determine if user needs cold-start recommendations",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ColdStartStatusResponse> checkColdStartStatus(
        @AuthenticationPrincipal User currentUser
    ) {
        boolean isColdStart = coldStartService.isUserColdStart(currentUser.getId());
        String strategy = coldStartService.getRecommendedStrategy(currentUser.getId());

        return ResponseEntity.ok(ColdStartStatusResponse.builder()
            .isColdStart(isColdStart)
            .recommendedStrategy(strategy)
            .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ColdStartStatusResponse {
        private Boolean isColdStart;
        private String recommendedStrategy;
    }
}

