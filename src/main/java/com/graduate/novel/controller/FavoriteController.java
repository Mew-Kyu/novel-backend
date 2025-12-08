package com.graduate.novel.controller;

import com.graduate.novel.domain.favorite.FavoriteDto;
import com.graduate.novel.domain.favorite.FavoriteService;
import com.graduate.novel.domain.favorite.FavoriteStatusDto;
import com.graduate.novel.domain.user.User;
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
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Get all favorites for the current user
     * Requires: USER role
     */
    @GetMapping
    public ResponseEntity<Page<FavoriteDto>> getUserFavorites(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FavoriteDto> favorites = favoriteService.getUserFavorites(currentUser, pageable);
        return ResponseEntity.ok(favorites);
    }

    /**
     * Add a story to favorites
     * Requires: USER role
     */
    @PostMapping("/{storyId}")
    public ResponseEntity<FavoriteDto> addToFavorites(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User currentUser
    ) {
        FavoriteDto favorite = favoriteService.addToFavorites(storyId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    /**
     * Remove a story from favorites
     * Requires: USER role
     */
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> removeFromFavorites(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User currentUser
    ) {
        favoriteService.removeFromFavorites(storyId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if a story is in user's favorites and get favorite count
     * Requires: USER role
     */
    @GetMapping("/check/{storyId}")
    public ResponseEntity<FavoriteStatusDto> checkFavoriteStatus(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User currentUser
    ) {
        FavoriteStatusDto status = favoriteService.checkFavoriteStatus(storyId, currentUser);
        return ResponseEntity.ok(status);
    }

    /**
     * Get favorite count for a story (public endpoint)
     * No authentication required
     */
    @GetMapping("/count/{storyId}")
    public ResponseEntity<Long> getFavoriteCount(@PathVariable Long storyId) {
        long count = favoriteService.getFavoriteCount(storyId);
        return ResponseEntity.ok(count);
    }
}

