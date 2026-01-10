package com.graduate.novel.controller;

import com.graduate.novel.domain.user.UserProfileService;
import com.graduate.novel.domain.user.UserProfile;
import com.graduate.novel.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile Analytics", description = "APIs for user profile analytics and behavior metrics")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(
        summary = "Get user profile",
        description = "Get detailed user profile including reading metrics, genre diversity, and embeddings",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfile> getUserProfile(@AuthenticationPrincipal User user) {
        Long userId = user.getId();
        log.info("Getting profile for user {}", userId);

        UserProfile profile = userProfileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh user profile",
        description = "Manually trigger profile update with latest reading behavior and embeddings",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserProfile> refreshProfile(@AuthenticationPrincipal User user) {
        Long userId = user.getId();
        log.info("Refreshing profile for user {}", userId);

        UserProfile profile = userProfileService.updateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/refresh-embedding")
    @Operation(
        summary = "Refresh user profile embedding",
        description = "Update user profile embedding based on recent reading history with time decay",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> refreshProfileEmbedding(@AuthenticationPrincipal User user) {
        Long userId = user.getId();
        log.info("Refreshing profile embedding for user {}", userId);

        UserProfile profile = userProfileService.getOrCreateProfile(userId);
        userProfileService.updateProfileEmbedding(profile, userId);

        return ResponseEntity.ok().build();
    }
}

