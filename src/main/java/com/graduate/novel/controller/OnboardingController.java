package com.graduate.novel.controller;

import com.graduate.novel.domain.onboarding.OnboardingService;
import com.graduate.novel.domain.onboarding.UserOnboarding;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Onboarding", description = "APIs for new user onboarding and preference collection")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/preferences")
    @Operation(
        summary = "Save user onboarding preferences",
        description = "Save user preferences collected during first-time onboarding. Used for cold-start recommendations.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserOnboarding> savePreferences(
        @RequestBody OnboardingService.OnboardingRequest request,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Saving onboarding preferences for user {}", userId);

        UserOnboarding onboarding = onboardingService.saveOnboardingPreferences(userId, request);
        return ResponseEntity.ok(onboarding);
    }

    @GetMapping("/status")
    @Operation(
        summary = "Get onboarding status",
        description = "Check if user has completed onboarding",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<OnboardingStatusResponse> getStatus(@AuthenticationPrincipal User user) {
        Long userId = user.getId();

        UserOnboarding onboarding = onboardingService.getOnboardingStatus(userId);
        boolean completed = onboarding != null && onboarding.getCompleted();

        return ResponseEntity.ok(OnboardingStatusResponse.builder()
            .completed(completed)
            .onboarding(onboarding)
            .build());
    }

    @GetMapping("/recommendations")
    @Operation(
        summary = "Get onboarding-based recommendations",
        description = "Get personalized recommendations based on onboarding preferences",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<StoryDto>> getRecommendations(
        @RequestParam(defaultValue = "10") int limit,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Getting onboarding recommendations for user {}", userId);

        List<StoryDto> recommendations = onboardingService.getOnboardingRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OnboardingStatusResponse {
        private Boolean completed;
        private UserOnboarding onboarding;
    }
}

