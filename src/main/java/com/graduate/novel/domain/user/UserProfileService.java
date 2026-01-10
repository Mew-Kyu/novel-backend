package com.graduate.novel.domain.user;

import com.graduate.novel.ai.service.GeminiService;
import com.graduate.novel.domain.history.ReadingHistory;
import com.graduate.novel.domain.history.ReadingHistoryRepository;
import com.graduate.novel.domain.rating.RatingRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user profiles with temporal weighting and embeddings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ReadingHistoryRepository historyRepository;
    private final RatingRepository ratingRepository;
    private final StoryRepository storyRepository;
    private final GeminiService geminiService;

    private static final int PROFILE_STALENESS_THRESHOLD_DAYS = 7;
    private static final double TIME_DECAY_FACTOR = 0.1; // Exponential decay rate

    /**
     * Get or create user profile
     */
    @Transactional
    public UserProfile getOrCreateProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = new User();
                user.setId(userId);

                UserProfile profile = UserProfile.builder()
                    .user(user)
                    .totalStoriesRead(0)
                    .totalChaptersRead(0)
                    .averageCompletionRate(0.0)
                    .chaptersPerWeek(0.0)
                    .avgSessionDurationMinutes(0.0)
                    .genreDiversityScore(0.0)
                    .lastProfileUpdate(LocalDateTime.now())
                    .build();

                return userProfileRepository.save(profile);
            });
    }

    /**
     * Update user profile with latest behavior data
     */
    @Transactional
    public UserProfile updateProfile(Long userId) {
        log.info("Updating profile for user {}", userId);

        UserProfile profile = getOrCreateProfile(userId);

        // Update reading metrics
        updateReadingMetrics(profile, userId);

        // Update genre diversity
        updateGenreDiversity(profile, userId);

        // Update profile embedding (weighted average with time decay)
        updateProfileEmbedding(profile, userId);

        profile.setLastProfileUpdate(LocalDateTime.now());

        return userProfileRepository.save(profile);
    }

    /**
     * Calculate and update reading behavior metrics
     */
    private void updateReadingMetrics(UserProfile profile, Long userId) {
        // Get reading history
        var historyPage = historyRepository.findByUserIdOrderByLastReadAtDesc(
            userId, PageRequest.of(0, 500)
        );
        List<ReadingHistory> history = historyPage.getContent();

        if (history.isEmpty()) {
            log.debug("No reading history for user {}", userId);
            return;
        }

        // Total stories read
        Set<Long> uniqueStories = history.stream()
            .map(h -> h.getStory().getId())
            .collect(Collectors.toSet());
        profile.setTotalStoriesRead(uniqueStories.size());

        // Total chapters read (count non-null chapters)
        long chaptersRead = history.stream()
            .filter(h -> h.getChapter() != null)
            .count();
        profile.setTotalChaptersRead((int) chaptersRead);

        // Average completion rate (stories with progress >= 90% are considered completed)
        long completedStories = history.stream()
            .filter(h -> h.getProgressPercent() != null && h.getProgressPercent() >= 90)
            .map(h -> h.getStory().getId())
            .distinct()
            .count();

        double completionRate = uniqueStories.isEmpty() ? 0.0 :
            (double) completedStories / uniqueStories.size() * 100.0;
        profile.setAverageCompletionRate(completionRate);

        // Reading velocity (chapters per week in last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentChapters = history.stream()
            .filter(h -> h.getLastReadAt() != null && h.getLastReadAt().isAfter(thirtyDaysAgo))
            .filter(h -> h.getChapter() != null)
            .count();

        double chaptersPerWeek = (double) recentChapters / 4.3; // 30 days ≈ 4.3 weeks
        profile.setChaptersPerWeek(chaptersPerWeek);

        log.debug("Updated metrics for user {}: {} stories, {} chapters, {:.2f}% completion, {:.2f} ch/week",
            userId, profile.getTotalStoriesRead(), profile.getTotalChaptersRead(),
            completionRate, chaptersPerWeek);
    }

    /**
     * Calculate genre diversity score
     * Shannon entropy of genre distribution
     */
    private void updateGenreDiversity(UserProfile profile, Long userId) {
        var historyPage = historyRepository.findByUserIdOrderByLastReadAtDesc(
            userId, PageRequest.of(0, 200)
        );

        Map<Long, Integer> genreCounts = new HashMap<>();
        int totalGenres = 0;

        for (ReadingHistory h : historyPage.getContent()) {
            if (h.getStory() != null && h.getStory().getGenres() != null) {
                for (var genre : h.getStory().getGenres()) {
                    genreCounts.merge(genre.getId(), 1, Integer::sum);
                    totalGenres++;
                }
            }
        }

        if (totalGenres == 0) {
            profile.setGenreDiversityScore(0.0);
            return;
        }

        // Calculate Shannon entropy: -Σ(p_i * log2(p_i))
        double entropy = 0.0;
        for (int count : genreCounts.values()) {
            double p = (double) count / totalGenres;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        // Normalize to 0-1 range (max entropy for uniform distribution)
        double maxEntropy = Math.log(genreCounts.size()) / Math.log(2);
        double diversityScore = maxEntropy > 0 ? entropy / maxEntropy : 0.0;

        profile.setGenreDiversityScore(Math.min(1.0, Math.max(0.0, diversityScore)));

        log.debug("Genre diversity for user {}: {:.4f} (unique genres: {})",
            userId, diversityScore, genreCounts.size());
    }

    /**
     * Update profile embedding as weighted average of story embeddings
     * with exponential time decay (recent stories have higher weight)
     */
    @Transactional
    public void updateProfileEmbedding(UserProfile profile, Long userId) {
        log.info("Updating profile embedding for user {}", userId);

        // Get user's interactions with stories that have embeddings
        var historyPage = historyRepository.findByUserIdOrderByLastReadAtDesc(
            userId, PageRequest.of(0, 100)
        );
        var ratingsPage = ratingRepository.findByUserId(userId, PageRequest.of(0, 100));

        // Collect story IDs with their timestamps and weights
        Map<Long, StoryInteraction> storyInteractions = new HashMap<>();

        // From reading history
        for (ReadingHistory h : historyPage.getContent()) {
            if (h.getStory() != null && h.getLastReadAt() != null) {
                Long storyId = h.getStory().getId();
                double weight = 1.0; // Base weight from reading

                // Bonus weight for completion
                if (h.getProgressPercent() != null && h.getProgressPercent() >= 90) {
                    weight *= 1.5;
                }

                storyInteractions.merge(storyId,
                    new StoryInteraction(storyId, h.getLastReadAt(), weight),
                    (existing, newOne) -> {
                        // Keep most recent timestamp and sum weights
                        LocalDateTime latest = existing.timestamp.isAfter(newOne.timestamp) ?
                            existing.timestamp : newOne.timestamp;
                        return new StoryInteraction(storyId, latest, existing.weight + newOne.weight);
                    });
            }
        }

        // From ratings (higher weight for high ratings)
        for (var rating : ratingsPage.getContent()) {
            if (rating.getStory() != null && rating.getCreatedAt() != null) {
                Long storyId = rating.getStory().getId();
                double weight = rating.getRating() >= 4 ? 2.0 :
                               rating.getRating() == 3 ? 0.5 : -0.5;

                storyInteractions.merge(storyId,
                    new StoryInteraction(storyId, rating.getCreatedAt(), weight),
                    (existing, newOne) -> new StoryInteraction(
                        storyId,
                        existing.timestamp.isAfter(newOne.timestamp) ? existing.timestamp : newOne.timestamp,
                        existing.weight + newOne.weight
                    ));
            }
        }

        if (storyInteractions.isEmpty()) {
            log.debug("No story interactions for user {}, cannot update profile embedding", userId);
            return;
        }

        // Fetch stories with embeddings
        List<Long> storyIds = new ArrayList<>(storyInteractions.keySet());
        List<Story> stories = storyRepository.findByIdInWithGenres(storyIds).stream()
            .filter(s -> s.getEmbedding() != null && !s.getEmbedding().isEmpty())
            .collect(Collectors.toList());

        if (stories.isEmpty()) {
            log.debug("No stories with embeddings for user {}", userId);
            return;
        }

        // Calculate weighted average embedding with time decay
        LocalDateTime now = LocalDateTime.now();
        float[] weightedSum = null;
        double totalWeight = 0.0;

        for (Story story : stories) {
            StoryInteraction interaction = storyInteractions.get(story.getId());
            if (interaction == null) continue;

            // Parse embedding
            float[] embedding = parseEmbedding(story.getEmbedding());
            if (embedding == null) continue;

            // Calculate time decay weight (exponential decay)
            long daysSinceInteraction = ChronoUnit.DAYS.between(interaction.timestamp, now);
            double timeDecay = Math.exp(-TIME_DECAY_FACTOR * daysSinceInteraction);

            // Final weight = interaction weight * time decay
            double finalWeight = interaction.weight * timeDecay;

            if (finalWeight <= 0) continue;

            // Initialize or add to weighted sum
            if (weightedSum == null) {
                weightedSum = new float[embedding.length];
            }

            for (int i = 0; i < embedding.length; i++) {
                weightedSum[i] += (float)(embedding[i] * finalWeight);
            }

            totalWeight += finalWeight;
        }

        if (weightedSum == null || totalWeight == 0) {
            log.debug("Could not calculate weighted embedding for user {}", userId);
            return;
        }

        // Normalize by total weight
        for (int i = 0; i < weightedSum.length; i++) {
            weightedSum[i] = (float)(weightedSum[i] / totalWeight);
        }

        // Convert to vector string and save
        String vectorString = convertFloatArrayToVectorString(weightedSum);
        userProfileRepository.updateEmbedding(userId, vectorString);

        log.info("Updated profile embedding for user {} from {} stories (total weight: {})",
            userId, stories.size(), String.format("%.2f", totalWeight));
    }

    /**
     * Refresh stale profiles asynchronously
     */
    @Async
    @Transactional
    public void refreshStaleProfiles() {
        log.info("Refreshing stale user profiles (older than {} days)", PROFILE_STALENESS_THRESHOLD_DAYS);

        List<UserProfile> staleProfiles = userProfileRepository.findStaleProfiles(PROFILE_STALENESS_THRESHOLD_DAYS);

        log.info("Found {} stale profiles to refresh", staleProfiles.size());

        int updated = 0;
        for (UserProfile profile : staleProfiles) {
            try {
                updateProfile(profile.getUser().getId());
                updated++;
            } catch (Exception e) {
                log.error("Failed to update profile for user {}: {}",
                    profile.getUser().getId(), e.getMessage());
            }
        }

        log.info("Refreshed {}/{} stale profiles", updated, staleProfiles.size());
    }

    // ========== Helper Methods ==========

    private static class StoryInteraction {
        Long storyId;
        LocalDateTime timestamp;
        double weight;

        StoryInteraction(Long storyId, LocalDateTime timestamp, double weight) {
            this.storyId = storyId;
            this.timestamp = timestamp;
            this.weight = weight;
        }
    }

    /**
     * Parse vector string to float array
     */
    private float[] parseEmbedding(String embeddingStr) {
        if (embeddingStr == null || embeddingStr.isEmpty() || embeddingStr.equals("[]")) {
            return null;
        }

        try {
            // Remove brackets and split
            String cleaned = embeddingStr.replace("[", "").replace("]", "").trim();
            if (cleaned.isEmpty()) return null;

            String[] parts = cleaned.split(",");
            float[] result = new float[parts.length];

            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }

            return result;
        } catch (Exception e) {
            log.warn("Failed to parse embedding: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert float array to PostgreSQL vector string format
     */
    private String convertFloatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}

