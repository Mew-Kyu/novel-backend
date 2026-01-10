package com.graduate.novel.domain.onboarding;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_onboarding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "preferred_genres", columnDefinition = "TEXT")
    private String preferredGenres;

    @Column(name = "reading_frequency")
    @Enumerated(EnumType.STRING)
    private ReadingFrequency readingFrequency;

    @Column(name = "preferred_length")
    @Enumerated(EnumType.STRING)
    private StoryLength preferredLength;

    @Column(name = "completion_preference")
    @Enumerated(EnumType.STRING)
    private CompletionPreference completionPreference;

    @Column(name = "exploration_preference")
    @Enumerated(EnumType.STRING)
    private ExplorationPreference explorationPreference;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ReadingFrequency {
        DAILY, WEEKLY, MONTHLY, CASUAL
    }

    public enum StoryLength {
        SHORT, MEDIUM, LONG, EPIC, ANY
    }

    public enum CompletionPreference {
        COMPLETIONIST, EXPLORER, BALANCED
    }

    public enum ExplorationPreference {
        SAFE, ADVENTUROUS, BALANCED
    }
}

