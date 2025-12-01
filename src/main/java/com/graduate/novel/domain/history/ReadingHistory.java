package com.graduate.novel.domain.history;

import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_history_read")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "scroll_offset")
    @Builder.Default
    private Integer scrollOffset = 0;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @PrePersist
    protected void onCreate() {
        lastReadAt = LocalDateTime.now();
        if (progressPercent == null) {
            progressPercent = 0;
        }
        if (scrollOffset == null) {
            scrollOffset = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastReadAt = LocalDateTime.now();
    }
}
