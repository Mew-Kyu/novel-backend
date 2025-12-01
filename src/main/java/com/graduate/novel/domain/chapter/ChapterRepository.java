package com.graduate.novel.domain.chapter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByStoryIdOrderByChapterIndexAsc(Long storyId);
    Optional<Chapter> findByStoryIdAndId(Long storyId, Long chapterId);
    Optional<Chapter> findByStoryIdAndChapterIndex(Long storyId, Integer chapterIndex);
    boolean existsByStoryIdAndChapterIndex(Long storyId, Integer chapterIndex);
    List<Chapter> findByCrawlStatus(String status);
    List<Chapter> findByTranslateStatus(String status);
    List<Chapter> findByStoryIdAndTranslateStatus(Long storyId, String status);
    List<Chapter> findByStoryIdAndTranslateStatusAndRawContentIsNotNull(Long storyId, String status);
}
