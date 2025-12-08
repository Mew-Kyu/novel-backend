package com.graduate.novel.domain.favorite;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.FavoriteMapper;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StoryRepository storyRepository;
    private final FavoriteMapper favoriteMapper;

    /**
     * Get all favorites for the current user
     */
    @Transactional(readOnly = true)
    public Page<FavoriteDto> getUserFavorites(User currentUser, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return favorites.map(favoriteMapper::toDto);
    }

    /**
     * Add a story to favorites
     */
    @Transactional
    public FavoriteDto addToFavorites(Long storyId, User currentUser) {
        // Check if story exists
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new IllegalStateException("Story is already in favorites");
        }

        // Create new favorite
        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .story(story)
                .build();

        favorite = favoriteRepository.save(favorite);
        return favoriteMapper.toDto(favorite);
    }

    /**
     * Remove a story from favorites
     */
    @Transactional
    public void removeFromFavorites(Long storyId, User currentUser) {
        // Check if favorite exists
        if (!favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new ResourceNotFoundException("Story is not in favorites");
        }

        favoriteRepository.deleteByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    /**
     * Check if a story is in user's favorites
     */
    @Transactional(readOnly = true)
    public FavoriteStatusDto checkFavoriteStatus(Long storyId, User currentUser) {
        boolean isFavorite = favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
        long favoriteCount = favoriteRepository.countByStoryId(storyId);

        return new FavoriteStatusDto(isFavorite, favoriteCount);
    }

    /**
     * Get favorite count for a story (public, no auth required)
     */
    @Transactional(readOnly = true)
    public long getFavoriteCount(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
    }
}

