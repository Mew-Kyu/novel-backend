package com.graduate.novel.domain.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * Find a favorite by user ID and story ID
     */
    Optional<Favorite> findByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Check if a user has favorited a story
     */
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Get all favorites for a user with pagination
     */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.story WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Delete a favorite by user ID and story ID
     */
    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    /**
     * Count favorites for a story
     */
    long countByStoryId(Long storyId);
}

