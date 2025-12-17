package com.graduate.novel.domain.chapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByStoryIdOrderByChapterIndexAsc(Long storyId);
    List<Chapter> findByStoryIdAndChapterIndexBetweenOrderByChapterIndexAsc(Long storyId, Integer startIndex, Integer endIndex);
    Optional<Chapter> findByStoryIdAndId(Long storyId, Long chapterId);
    Optional<Chapter> findByStoryIdAndChapterIndex(Long storyId, Integer chapterIndex);
    boolean existsByStoryIdAndChapterIndex(Long storyId, Integer chapterIndex);
    List<Chapter> findByCrawlStatus(String status);
    List<Chapter> findByTranslateStatus(String status);
    List<Chapter> findByStoryIdAndTranslateStatus(Long storyId, String status);
    List<Chapter> findByStoryIdAndTranslateStatusAndRawContentIsNotNull(Long storyId, String status);

    // Homepage features

    // Count chapters by story ID
    @Query("SELECT COUNT(c) FROM Chapter c WHERE c.story.id = :storyId")
    Integer countByStoryId(@Param("storyId") Long storyId);

    // Get latest chapter of a story
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId ORDER BY c.updatedAt DESC, c.chapterIndex DESC")
    List<Chapter> findLatestByStoryId(@Param("storyId") Long storyId, Pageable pageable);

    // Get latest chapters across all stories
    @Query("SELECT c FROM Chapter c ORDER BY c.updatedAt DESC")
    List<Chapter> findLatestChapters(Pageable pageable);
}
