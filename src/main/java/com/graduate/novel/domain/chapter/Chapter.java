package com.graduate.novel.domain.chapter;

import com.graduate.novel.domain.story.Story;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "story_chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "chapter_index", nullable = false)
    private Integer chapterIndex;

    @Column(columnDefinition = "TEXT")
    private String title;

    // RAW content (original Japanese from syosetu)
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "crawl_status", length = 50)
    @Builder.Default
    private String crawlStatus = "PENDING"; // PENDING / SUCCESS / FAILED

    @Column(name = "crawl_time")
    private LocalDateTime crawlTime;

    // TRANSLATED content
    @Column(name = "translated_content", columnDefinition = "TEXT")
    private String translatedContent;

    @Column(name = "translate_status", length = 50)
    @Builder.Default
    private String translateStatus = "NONE"; // NONE / PENDING / SUCCESS / FAILED

    @Column(name = "translate_time")
    private LocalDateTime translateTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (crawlStatus == null) {
            crawlStatus = "PENDING";
        }
        if (translateStatus == null) {
            translateStatus = "NONE";
        }
    }
}
