package com.graduate.novel.domain.crawljob;

import com.graduate.novel.domain.base.BaseEntity;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.chapter.Chapter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "crawl_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CrawlJob extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType; // CRAWL / TRANSLATE

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING / PROCESSING / SUCCESS / FAILED

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (attempts == null) {
            attempts = 0;
        }
    }
}

