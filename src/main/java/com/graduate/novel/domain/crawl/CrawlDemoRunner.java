package com.graduate.novel.domain.crawl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Example command line runner to demonstrate crawling usage.
 *
 * Enable by adding to application.properties:
 * crawl.demo.enabled=true
 * crawl.demo.novel-url=https://ncode.syosetu.com/n5375cy/
 * crawl.demo.start-chapter=1
 * crawl.demo.end-chapter=3
 */
@Component
@ConditionalOnProperty(name = "crawl.demo.enabled", havingValue = "true")
@Slf4j
public class CrawlDemoRunner implements CommandLineRunner {

    private final SyosetuCrawlService crawlService;

    public CrawlDemoRunner(SyosetuCrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Override
    public void run(String... args) {
        log.info("=== Syosetu Crawl Demo Starting ===");

        // Example 1: Crawl only first chapter
        CrawlNovelRequest request1 = CrawlNovelRequest.builder()
                .novelUrl("https://ncode.syosetu.com/n5375cy/")
                .build();

        try {
            log.info("Example 1: Crawling first chapter only...");
            CrawlNovelResponse response1 = crawlService.crawlNovel(request1);
            log.info("Result: {}", response1);
        } catch (Exception e) {
            log.error("Example 1 failed: {}", e.getMessage());
        }

        // Example 2: Crawl multiple chapters
        CrawlNovelRequest request2 = CrawlNovelRequest.builder()
                .novelUrl("https://ncode.syosetu.com/n5375cy/")
                .startChapter(1)
                .endChapter(3)
                .build();

        try {
            log.info("Example 2: Crawling chapters 1-3...");
            CrawlNovelResponse response2 = crawlService.crawlNovel(request2);
            log.info("Result: {}", response2);
        } catch (Exception e) {
            log.error("Example 2 failed: {}", e.getMessage());
        }

        log.info("=== Syosetu Crawl Demo Completed ===");
    }
}

