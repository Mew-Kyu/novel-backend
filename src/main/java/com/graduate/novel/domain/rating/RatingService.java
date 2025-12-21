package com.graduate.novel.domain.rating;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.RatingMapper;
import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;
    private final EntityManager entityManager;

    @Transactional
    public RatingDto createOrUpdateRating(Long userId, CreateRatingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + request.storyId()));

        Rating rating = ratingRepository.findByUserIdAndStoryId(userId, request.storyId())
                .orElse(Rating.builder()
                        .user(user)
                        .story(story)
                        .build());

        rating.setRating(request.rating());
        rating = ratingRepository.save(rating);

        // Update story's average rating cache
        updateStoryRatingCache(request.storyId());

        return ratingMapper.toDto(rating);
    }

    @Transactional
    public RatingDto updateRating(Long userId, Long ratingId, UpdateRatingRequest request) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + ratingId));

        // Only the owner can update their rating (no moderator override)
        if (!rating.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only update your own ratings");
        }

        Long storyId = rating.getStory().getId();
        rating.setRating(request.rating());
        rating = ratingRepository.save(rating);

        // Update story's average rating cache
        updateStoryRatingCache(storyId);

        return ratingMapper.toDto(rating);
    }

    @Transactional
    public void deleteRating(Long userId, Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + ratingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Allow deletion if user owns the rating OR user is ADMIN/MODERATOR
        if (!rating.getUser().getId().equals(userId) && !hasModeratorAccess(user)) {
            throw new AccessDeniedException("You can only delete your own ratings");
        }

        Long storyId = rating.getStory().getId();
        ratingRepository.delete(rating);

        // Update story's average rating cache
        updateStoryRatingCache(storyId);
    }

    /**
     * Check if user has ADMIN or MODERATOR role
     */
    private boolean hasModeratorAccess(User user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR);
    }

    @Transactional(readOnly = true)
    public Page<RatingDto> getRatingsByStory(Long storyId, Pageable pageable) {
        return ratingRepository.findByStoryId(storyId, pageable)
                .map(ratingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RatingDto> getRatingsByUser(Long userId, Pageable pageable) {
        return ratingRepository.findByUserId(userId, pageable)
                .map(ratingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RatingDto getUserRatingForStory(Long userId, Long storyId) {
        Rating rating = ratingRepository.findByUserIdAndStoryId(userId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rating not found for user " + userId + " and story " + storyId));
        return ratingMapper.toDto(rating);
    }

    @Transactional(readOnly = true)
    public StoryRatingDto getStoryRating(Long storyId) {
        // Verify story exists
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        Double avgRating = ratingRepository.getAverageRatingByStoryId(storyId);
        Long totalRatings = ratingRepository.getTotalRatingsByStoryId(storyId);

        // Round to 1 decimal place
        Double roundedAverage = null;
        if (avgRating != null) {
            roundedAverage = BigDecimal.valueOf(avgRating)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new StoryRatingDto(storyId, roundedAverage, totalRatings);
    }

    /**
     * Update story's averageRating and totalRatings cache
     * Called automatically after any rating change
     * Uses REQUIRES_NEW to ensure cache is committed immediately
     * Clears story detail cache to ensure fresh data on next request
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = {
        @CacheEvict(value = "storyDetails", key = "#storyId"),
        @CacheEvict(value = "featuredStories", allEntries = true),
        @CacheEvict(value = "trendingStories", allEntries = true)
    })
    protected void updateStoryRatingCache(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        // Optimized: Get both AVG and COUNT in a single query
        RatingStats stats = ratingRepository.getRatingStatsByStoryId(storyId);

        // Round to 1 decimal place
        Double roundedAverage = null;
        if (stats.averageRating() != null) {
            roundedAverage = BigDecimal.valueOf(stats.averageRating())
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        story.setAverageRating(roundedAverage);
        story.setTotalRatings(stats.totalRatings());
        storyRepository.saveAndFlush(story); // Flush immediately to ensure cache is persisted

        // Clear EntityManager cache to ensure subsequent queries get fresh data
        entityManager.clear();
    }

    @Transactional(readOnly = true)
    public Page<RatingDto> getAllRatings(Pageable pageable) {
        return ratingRepository.findAll(pageable)
                .map(ratingMapper::toDto);
    }
}

