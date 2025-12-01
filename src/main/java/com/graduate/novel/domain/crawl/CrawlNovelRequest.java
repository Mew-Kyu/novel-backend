package com.graduate.novel.domain.crawl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlNovelRequest {

    @NotBlank(message = "Novel URL is required")
    private String novelUrl;

    @Min(value = 1, message = "Start chapter must be at least 1")
    private Integer startChapter;

    @Min(value = 1, message = "End chapter must be at least 1")
    private Integer endChapter;
}

