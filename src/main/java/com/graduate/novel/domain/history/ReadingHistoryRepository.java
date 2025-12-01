package com.graduate.novel.domain.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Page<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId, Pageable pageable);
    Optional<ReadingHistory> findByUserIdAndStoryId(Long userId, Long storyId);
}
