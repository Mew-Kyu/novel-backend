package com.graduate.novel.domain.crawljob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {
    List<CrawlJob> findByStatus(String status);
    List<CrawlJob> findByStoryIdAndJobType(Long storyId, String jobType);
    List<CrawlJob> findByChapterId(Long chapterId);
}

