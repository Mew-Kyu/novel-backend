package com.graduate.novel.domain.history;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Page<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId, Pageable pageable);

    // Use pessimistic write lock to prevent concurrent inserts
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM ReadingHistory h WHERE h.user.id = :userId AND h.story.id = :storyId ORDER BY h.lastReadAt DESC LIMIT 1")
    Optional<ReadingHistory> findByUserIdAndStoryIdWithLock(@Param("userId") Long userId, @Param("storyId") Long storyId);

    // Keep the non-locking version for read-only operations
    @Query("SELECT h FROM ReadingHistory h WHERE h.user.id = :userId AND h.story.id = :storyId ORDER BY h.lastReadAt DESC LIMIT 1")
    Optional<ReadingHistory> findByUserIdAndStoryId(@Param("userId") Long userId, @Param("storyId") Long storyId);

    // Count methods for cold-start detection
    long countByUserId(Long userId);
}
