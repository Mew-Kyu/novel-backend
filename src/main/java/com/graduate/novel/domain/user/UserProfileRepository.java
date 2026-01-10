package com.graduate.novel.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Query("SELECT up FROM UserProfile up WHERE up.user.id = :userId")
    Optional<UserProfile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserProfile up WHERE up.profileEmbedding IS NULL")
    List<UserProfile> findProfilesWithoutEmbedding();

    @Query("SELECT up FROM UserProfile up WHERE up.lastProfileUpdate < CURRENT_TIMESTAMP - :days DAY")
    List<UserProfile> findStaleProfiles(@Param("days") int days);

    @Modifying
    @Query(value = "UPDATE user_profiles SET profile_embedding = CAST(:embeddingString AS vector) WHERE user_id = :userId",
        nativeQuery = true)
    void updateEmbedding(@Param("userId") Long userId,
                        @Param("embeddingString") String embeddingString);

    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.profileEmbedding IS NOT NULL")
    long countProfilesWithEmbedding();

    @Query("SELECT AVG(up.averageCompletionRate) FROM UserProfile up WHERE up.totalStoriesRead > 0")
    Double getAverageCompletionRate();

    @Query("SELECT AVG(up.chaptersPerWeek) FROM UserProfile up WHERE up.totalChaptersRead > 0")
    Double getAverageReadingVelocity();
}

