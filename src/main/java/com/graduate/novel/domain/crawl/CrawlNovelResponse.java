package com.graduate.novel.domain.crawl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlNovelResponse {
    private Long storyId;
    private String title;
    private String description;
    private Integer chaptersCrawled;
    private Integer chaptersSucceeded;
    private Integer chaptersFailed;
    private String message;
}

