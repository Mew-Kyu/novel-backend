package com.graduate.novel.domain.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndStoryId(Long userId, Long storyId);

    Page<Rating> findByStoryId(Long storyId, Pageable pageable);

    Page<Rating> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.story.id = :storyId")
    Double getAverageRatingByStoryId(@Param("storyId") Long storyId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.story.id = :storyId")
    Long getTotalRatingsByStoryId(@Param("storyId") Long storyId);
}

