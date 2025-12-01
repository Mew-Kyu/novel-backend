package com.graduate.novel.domain.story;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (sourceSite == null) {
            sourceSite = "syosetu";
        }
    }
}

