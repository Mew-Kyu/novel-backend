package com.graduate.novel.ai.service;

import com.graduate.novel.common.exception.RateLimitExceededException;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final GeminiService geminiService;
    private final StoryRepository storyRepository;

    /**
     * Generate and save embedding for a story
     * Note: RateLimitExceededException will be caught by GlobalExceptionHandler
     */
    @Transactional
    public void generateStoryEmbedding(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + storyId));

        log.info("Generating embedding for story: {}", story.getTitle());

        // Create a combined text for embedding (title + description)
        String textForEmbedding = buildEmbeddingText(story);

        try {
            // Generate embedding using Gemini (may throw RateLimitExceededException)
            float[] embedding = geminiService.generateEmbedding(textForEmbedding);

            if (embedding != null) {
                // Convert float[] to PostgreSQL vector format string
                String vectorString = convertFloatArrayToVectorString(embedding);

                // Use custom query with explicit CAST to avoid type error
                storyRepository.updateEmbedding(story.getId(), vectorString);

                log.info("Embedding saved successfully for story: {}", story.getTitle());
            } else {
                log.error("Failed to generate embedding for story: {}", story.getTitle());
                throw new RuntimeException("Failed to generate embedding");
            }
        } catch (RateLimitExceededException e) {
            // Re-throw to let GlobalExceptionHandler handle it
            log.warn("Rate limit exceeded while generating embedding for story: {}", story.getTitle());
            throw e;
        } catch (Exception e) {
            log.error("Error generating embedding for story {}: {}", story.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generate embeddings for all stories without embeddings
     * This is a batch operation - handles rate limits internally with retry logic
     */
    @Transactional
    public void generateAllMissingEmbeddings() {
        List<Story> storiesWithoutEmbedding = storyRepository.findStoriesWithoutEmbedding();
        log.info("Found {} stories without embeddings", storiesWithoutEmbedding.size());

        int successCount = 0;
        int failCount = 0;
        int rateLimitCount = 0;

        for (Story story : storiesWithoutEmbedding) {
            try {
                log.info("Processing story {}/{}: {}",
                        successCount + failCount + rateLimitCount + 1,
                        storiesWithoutEmbedding.size(),
                        story.getTitle());

                String textForEmbedding = buildEmbeddingText(story);

                // Log the text being processed (truncated)
                String preview = textForEmbedding.length() > 100
                        ? textForEmbedding.substring(0, 100) + "..."
                        : textForEmbedding;
                log.debug("Embedding text preview: {}", preview);

                float[] embedding = geminiService.generateEmbedding(textForEmbedding);

                if (embedding != null) {
                    // Convert float[] to PostgreSQL vector format string
                    String vectorString = convertFloatArrayToVectorString(embedding);

                    // Use custom query with explicit CAST
                    storyRepository.updateEmbedding(story.getId(), vectorString);

                    successCount++;
                    log.info("✓ Generated embedding for story: {} ({}/{})",
                            story.getTitle(), successCount + failCount + rateLimitCount, storiesWithoutEmbedding.size());
                } else {
                    failCount++;
                    log.error("✗ Failed to generate embedding for story: {}", story.getTitle());
                }

                // Add delay to avoid rate limiting (2 seconds)
                if (successCount + failCount + rateLimitCount < storiesWithoutEmbedding.size()) {
                    log.debug("Waiting 2 seconds before next request to avoid rate limits...");
                    Thread.sleep(2000);
                }

            } catch (RateLimitExceededException e) {
                rateLimitCount++;
                long waitSeconds = e.getRetryAfterSeconds() > 0 ? e.getRetryAfterSeconds() : 60;
                log.warn("✗ Rate limit exceeded for story: {}. Waiting {} seconds...",
                        story.getTitle(), waitSeconds);
                log.warn("Rate limit message: {}", e.getMessage());

                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for rate limit cooldown");
                    break; // Stop processing if interrupted
                }

            } catch (Exception e) {
                failCount++;
                log.error("✗ Error generating embedding for story {}: {}",
                        story.getTitle(), e.getMessage());
                log.debug("Full error trace:", e);
            }
        }

        log.info("Embedding generation completed. Success: {}, Failed: {}, Rate Limited: {}",
                successCount, failCount, rateLimitCount);

        if (rateLimitCount > 0) {
            log.warn("⚠️ {} stories were skipped due to rate limiting. Please run this job again later.",
                    rateLimitCount);
        }
    }

    /**
     * Search stories by semantic similarity
     * Note: RateLimitExceededException will be caught by GlobalExceptionHandler
     */
    @Transactional(readOnly = true)
    public List<Story> searchBySimilarity(String query, int limit) {
        log.info("Performing semantic search for query: '{}'", query);

        try {
            // Generate embedding for the search query (may throw RateLimitExceededException)
            float[] queryEmbedding = geminiService.generateEmbedding(query);

            if (queryEmbedding == null) {
                log.error("Failed to generate embedding for query");
                throw new RuntimeException("Failed to generate query embedding");
            }

            // Convert to PostgreSQL vector format string
            String queryEmbeddingString = convertFloatArrayToVectorString(queryEmbedding);

            // Step 1: Find story IDs using vector similarity (native query required for vector operations)
            List<Long> storyIds = storyRepository.findStoryIdsBySimilarity(queryEmbeddingString, limit);

            if (storyIds.isEmpty()) {
                log.info("No similar stories found");
                return List.of();
            }

            // Step 2: Fetch full Story entities with genres eagerly loaded to avoid lazy initialization error
            List<Story> results = storyRepository.findByIdInWithGenres(storyIds);

            log.info("Found {} similar stories", results.size());
            return results;

        } catch (RateLimitExceededException e) {
            // Re-throw to let GlobalExceptionHandler handle it
            log.warn("Rate limit exceeded during semantic search for query: '{}'", query);
            throw e;
        } catch (Exception e) {
            log.error("Error during semantic search for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Failed to perform semantic search: " + e.getMessage(), e);
        }
    }

    /**
     * Build text for embedding from story data
     */
    private String buildEmbeddingText(Story story) {
        StringBuilder sb = new StringBuilder();

        if (story.getTitle() != null) {
            sb.append("Title: ").append(story.getTitle()).append("\n");
        }

        if (story.getAuthorName() != null) {
            sb.append("Author: ").append(story.getAuthorName()).append("\n");
        }

        if (story.getDescription() != null) {
            sb.append("Description: ").append(story.getDescription());
        }

        return sb.toString();
    }

    /**
     * Refresh embedding for an existing story (useful after updates)
     */
    @Transactional
    public void refreshStoryEmbedding(Long storyId) {
        log.info("Refreshing embedding for story id: {}", storyId);
        generateStoryEmbedding(storyId);
    }

    /**
     * Convert float array to PostgreSQL vector format string
     */
    private String convertFloatArrayToVectorString(float[] array) {
        if (array == null || array.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");

        return sb.toString();
    }
}

