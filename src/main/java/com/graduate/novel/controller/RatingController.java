package com.graduate.novel.controller;

import com.graduate.novel.domain.rating.*;
import com.graduate.novel.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingDto> createOrUpdateRating(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRatingRequest request) {
        RatingDto rating = ratingService.createOrUpdateRating(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rating);
    }

    @PutMapping("/{ratingId}")
    public ResponseEntity<RatingDto> updateRating(
            @AuthenticationPrincipal User user,
            @PathVariable Long ratingId,
            @Valid @RequestBody UpdateRatingRequest request) {
        RatingDto rating = ratingService.updateRating(user.getId(), ratingId, request);
        return ResponseEntity.ok(rating);
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> deleteRating(
            @AuthenticationPrincipal User user,
            @PathVariable Long ratingId) {
        ratingService.deleteRating(user.getId(), ratingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<Page<RatingDto>> getRatingsByStory(
            @PathVariable Long storyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RatingDto> ratings = ratingService.getRatingsByStory(storyId, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/story/{storyId}/average")
    public ResponseEntity<StoryRatingDto> getStoryRating(@PathVariable Long storyId) {
        StoryRatingDto storyRating = ratingService.getStoryRating(storyId);
        return ResponseEntity.ok(storyRating);
    }

    @GetMapping("/user/me")
    public ResponseEntity<Page<RatingDto>> getMyRatings(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RatingDto> ratings = ratingService.getRatingsByUser(user.getId(), pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/story/{storyId}/me")
    public ResponseEntity<RatingDto> getMyRatingForStory(
            @AuthenticationPrincipal User user,
            @PathVariable Long storyId) {
        RatingDto rating = ratingService.getUserRatingForStory(user.getId(), storyId);
        return ResponseEntity.ok(rating);
    }
}

