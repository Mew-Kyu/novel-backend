package com.graduate.novel.domain.onboarding;

import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.story.StoryDto;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserOnboardingRepository onboardingRepository;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    @Transactional
    public UserOnboarding saveOnboardingPreferences(Long userId, OnboardingRequest request) {
        log.info("Saving onboarding preferences for user {}", userId);

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
            .orElse(new UserOnboarding());

        onboarding.setUserId(userId);
        onboarding.setPreferredGenres(request.getPreferredGenreIds() != null ?
            String.join(",", request.getPreferredGenreIds().stream()
                .map(String::valueOf)
                .collect(Collectors.toList())) : null);
        onboarding.setReadingFrequency(request.getReadingFrequency());
        onboarding.setPreferredLength(request.getPreferredLength());
        onboarding.setCompletionPreference(request.getCompletionPreference());
        onboarding.setExplorationPreference(request.getExplorationPreference());
        onboarding.setCompleted(true);

        return onboardingRepository.save(onboarding);
    }

    @Transactional(readOnly = true)
    public List<StoryDto> getOnboardingRecommendations(Long userId, int limit) {
        log.info("Getting onboarding-based recommendations for user {}", userId);

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
            .orElse(null);

        if (onboarding == null || !onboarding.getCompleted()) {
            log.debug("No onboarding data for user {}, returning empty list", userId);
            return List.of();
        }

        if (onboarding.getPreferredGenres() != null && !onboarding.getPreferredGenres().isEmpty()) {
            List<Long> genreIds = Arrays.stream(onboarding.getPreferredGenres().split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

            if (!genreIds.isEmpty()) {
                var stories = storyRepository.findByGenreId(
                    genreIds.get(0),
                    PageRequest.of(0, limit)
                );

                return stories.getContent().stream()
                    .map(storyMapper::toDto)
                    .collect(Collectors.toList());
            }
        }

        return List.of();
    }

    public boolean hasCompletedOnboarding(Long userId) {
        return onboardingRepository.findByUserId(userId)
            .map(UserOnboarding::getCompleted)
            .orElse(false);
    }

    public UserOnboarding getOnboardingStatus(Long userId) {
        return onboardingRepository.findByUserId(userId)
            .orElse(null);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OnboardingRequest {
        private List<Long> preferredGenreIds;
        private UserOnboarding.ReadingFrequency readingFrequency;
        private UserOnboarding.StoryLength preferredLength;
        private UserOnboarding.CompletionPreference completionPreference;
        private UserOnboarding.ExplorationPreference explorationPreference;
    }
}

