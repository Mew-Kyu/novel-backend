package com.graduate.novel.domain.crawl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Slf4j
public class CrawlController {

    private final SyosetuCrawlService syosetuCrawlService;

    /**
     * Crawl a novel from Syosetu
     *
     * @param request - Contains novelUrl, optional startChapter and endChapter
     * @return CrawlNovelResponse with crawl results
     */
    @PostMapping("/syosetu")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<CrawlNovelResponse> crawlSyosetuNovel(
            @Valid @RequestBody CrawlNovelRequest request) {

        log.info("Received crawl request for URL: {}", request.getNovelUrl());

        CrawlNovelResponse response = syosetuCrawlService.crawlNovel(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Health check for crawl service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Crawl service is running");
    }
}

