package com.graduate.novel.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User profile with aggregated behavior and preferences
 * Used for enhanced personalized recommendations
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    // Expose userId in JSON without lazy loading the entire User object
    @Transient
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    // Aggregated embedding from stories user interacted with (weighted average)
    // Note: insertable/updatable set to false because we use native queries with CAST to handle PostgreSQL vector type
    @Column(name = "profile_embedding", columnDefinition = "vector(768)", insertable = false, updatable = false)
    private String profileEmbedding; // Stored as vector in PostgreSQL

    // Reading behavior metrics
    @Column(name = "total_stories_read")
    @Builder.Default
    private Integer totalStoriesRead = 0;

    @Column(name = "total_chapters_read")
    @Builder.Default
    private Integer totalChaptersRead = 0;

    @Column(name = "average_completion_rate")
    @Builder.Default
    private Double averageCompletionRate = 0.0; // Percentage (0-100)

    // Reading velocity metrics
    @Column(name = "chapters_per_week")
    @Builder.Default
    private Double chaptersPerWeek = 0.0;

    @Column(name = "avg_session_duration_minutes")
    @Builder.Default
    private Double avgSessionDurationMinutes = 0.0;

    // Genre diversity
    @Column(name = "genre_diversity_score")
    @Builder.Default
    private Double genreDiversityScore = 0.0; // 0-1 scale

    // Temporal tracking
    @Column(name = "last_profile_update", nullable = false)
    private LocalDateTime lastProfileUpdate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        lastProfileUpdate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if profile needs refresh (stale)
     * @param daysThreshold number of days before profile is considered stale
     * @return true if profile should be refreshed
     */
    public boolean isStale(int daysThreshold) {
        if (lastProfileUpdate == null) return true;
        return lastProfileUpdate.plusDays(daysThreshold).isBefore(LocalDateTime.now());
    }
}

