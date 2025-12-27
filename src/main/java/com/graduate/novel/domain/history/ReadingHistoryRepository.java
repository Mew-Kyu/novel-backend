package com.graduate.novel.domain.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Page<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId, Pageable pageable);

    // Use query to get the most recent one in case of duplicates
    @Query("SELECT h FROM ReadingHistory h WHERE h.user.id = :userId AND h.story.id = :storyId ORDER BY h.lastReadAt DESC LIMIT 1")
    Optional<ReadingHistory> findByUserIdAndStoryId(@Param("userId") Long userId, @Param("storyId") Long storyId);
}
