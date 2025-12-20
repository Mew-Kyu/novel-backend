package com.graduate.novel.domain.story;

import com.graduate.novel.domain.genre.Genre;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "raw_title", columnDefinition = "TEXT")
    private String rawTitle;

    @Column(name = "translated_title", columnDefinition = "TEXT")
    private String translatedTitle;

    @Column(name = "author_name", columnDefinition = "TEXT")
    private String authorName;

    @Column(name = "raw_author_name", columnDefinition = "TEXT")
    private String rawAuthorName;

    @Column(name = "translated_author_name", columnDefinition = "TEXT")
    private String translatedAuthorName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "raw_description", columnDefinition = "TEXT")
    private String rawDescription;

    @Column(name = "translated_description", columnDefinition = "TEXT")
    private String translatedDescription;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    // PGVector embedding - stored as String in format "[0.1,0.2,...]" which PostgreSQL casts to vector
    // Using TEXT columnDefinition and marking as non-insertable/updatable to avoid Hibernate casting issues
    // Use StoryRepository.updateEmbedding() to update this field with proper CAST
    @Column(name = "embedding", columnDefinition = "TEXT", nullable = true, insertable = false, updatable = false)
    private String embedding;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_site", length = 255)
    @Builder.Default
    private String sourceSite = "syosetu";

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "story_genres",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "average_rating", columnDefinition = "NUMERIC(3,1)", nullable = true)
    @Builder.Default
    private Double averageRating = null;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Long totalRatings = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private StoryStatus status = StoryStatus.PUBLISHED;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sourceSite == null) {
            sourceSite = "syosetu";
        }
        if (viewCount == null) {
            viewCount = 0L;
        }
        if (featured == null) {
            featured = false;
        }
        if (status == null) {
            status = StoryStatus.PUBLISHED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

