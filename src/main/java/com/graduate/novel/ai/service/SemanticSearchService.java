package com.graduate.novel.ai.service;

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
     */
    @Transactional
    public void generateStoryEmbedding(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found with id: " + storyId));

        log.info("Generating embedding for story: {}", story.getTitle());

        // Create a combined text for embedding (title + description)
        String textForEmbedding = buildEmbeddingText(story);

        // Generate embedding using Gemini
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
    }

    /**
     * Generate embeddings for all stories without embeddings
     */
    @Transactional
    public void generateAllMissingEmbeddings() {
        List<Story> storiesWithoutEmbedding = storyRepository.findStoriesWithoutEmbedding();
        log.info("Found {} stories without embeddings", storiesWithoutEmbedding.size());

        int successCount = 0;
        int failCount = 0;

        for (Story story : storiesWithoutEmbedding) {
            try {
                log.info("Processing story {}/{}: {}",
                        successCount + failCount + 1,
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
                            story.getTitle(), successCount + failCount, storiesWithoutEmbedding.size());
                } else {
                    failCount++;
                    log.error("✗ Failed to generate embedding for story: {}", story.getTitle());
                }

                // Add a longer delay to avoid rate limiting (2 seconds)
                if (successCount + failCount < storiesWithoutEmbedding.size()) {
                    log.debug("Waiting 2 seconds before next request to avoid rate limits...");
                    Thread.sleep(2000);
                }

            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                failCount++;
                log.error("✗ Rate limit exceeded for story: {}. Waiting 60 seconds...", story.getTitle());
                try {
                    Thread.sleep(60000); // Wait 1 minute if rate limited
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {
                failCount++;
                log.error("✗ Error generating embedding for story {}: {}",
                        story.getTitle(), e.getMessage());
                log.debug("Full error trace:", e);
            }
        }

        log.info("Embedding generation completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * Search stories by semantic similarity
     */
    @Transactional(readOnly = true)
    public List<Story> searchBySimilarity(String query, int limit) {
        log.info("Performing semantic search for query: '{}'", query);

        // Generate embedding for the search query
        float[] queryEmbedding = geminiService.generateEmbedding(query);

        if (queryEmbedding == null) {
            log.error("Failed to generate embedding for query");
            throw new RuntimeException("Failed to generate query embedding");
        }

        // Convert to PostgreSQL vector format string
        String queryEmbeddingString = convertFloatArrayToVectorString(queryEmbedding);

        // Perform similarity search using cosine distance
        List<Story> results = storyRepository.findBySimilarity(queryEmbeddingString, limit);

        log.info("Found {} similar stories", results.size());
        return results;
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

