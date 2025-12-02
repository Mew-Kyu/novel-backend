package com.graduate.novel.domain.rating;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.RatingMapper;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

        return ratingMapper.toDto(rating);
    }

    @Transactional
    public RatingDto updateRating(Long userId, Long ratingId, UpdateRatingRequest request) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + ratingId));

        if (!rating.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own ratings");
        }

        rating.setRating(request.rating());
        rating = ratingRepository.save(rating);

        return ratingMapper.toDto(rating);
    }

    @Transactional
    public void deleteRating(Long userId, Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found with id: " + ratingId));

        if (!rating.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own ratings");
        }

        ratingRepository.delete(rating);
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
        storyRepository.findById(storyId)
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
}

