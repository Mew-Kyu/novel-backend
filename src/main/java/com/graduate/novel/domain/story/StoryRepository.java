package com.graduate.novel.domain.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    Page<Story> findByTitleContainingOrDescriptionContaining(
            String title, String description, Pageable pageable);

    List<Story> findBySourceSite(String sourceSite);
    List<Story> findBySourceUrl(String sourceUrl);

    // Find stories without embeddings
    @Query("SELECT s FROM Story s WHERE s.embedding IS NULL")
    List<Story> findStoriesWithoutEmbedding();

    // Update embedding with explicit CAST to vector type
    @Modifying
    @Query(value = "UPDATE stories SET embedding = CAST(:embeddingString AS vector) WHERE id = :storyId",
           nativeQuery = true)
    void updateEmbedding(@Param("storyId") Long storyId,
                        @Param("embeddingString") String embeddingString);

    // Semantic search using cosine similarity (1 - cosine distance)
   // queryEmbedding should be in format: "[0.1,0.2,0.3,...]"
    @Query(value = "SELECT * FROM stories " +
            "WHERE embedding IS NOT NULL " +
            "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
            "LIMIT :limit", nativeQuery = true)
    List<Story> findBySimilarity(@Param("queryEmbedding") String queryEmbedding,
                                  @Param("limit") int limit);
}
