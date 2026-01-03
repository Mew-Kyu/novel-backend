package com.graduate.novel.domain.crawljob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {
    List<CrawlJob> findByStatus(String status);
    List<CrawlJob> findByStoryIdAndJobType(Long storyId, String jobType);
    List<CrawlJob> findByChapterId(Long chapterId);

    // Check if there's an active job for a story (PENDING or PROCESSING)
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM CrawlJob j " +
           "WHERE j.story.id = :storyId AND j.jobType = :jobType " +
           "AND j.status IN ('PENDING', 'PROCESSING')")
    boolean existsActiveJobByStoryIdAndJobType(@Param("storyId") Long storyId, @Param("jobType") String jobType);

    // Check if there's an active job for a chapter (PENDING or PROCESSING)
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM CrawlJob j " +
           "WHERE j.chapter.id = :chapterId AND j.jobType = :jobType " +
           "AND j.status IN ('PENDING', 'PROCESSING')")
    boolean existsActiveJobByChapterIdAndJobType(@Param("chapterId") Long chapterId, @Param("jobType") String jobType);
}

