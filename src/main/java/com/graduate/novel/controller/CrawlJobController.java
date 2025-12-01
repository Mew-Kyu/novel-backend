package com.graduate.novel.controller;

import com.graduate.novel.domain.crawljob.CrawlJobDto;
import com.graduate.novel.domain.crawljob.CrawlJobService;
import com.graduate.novel.domain.crawljob.CreateCrawlJobRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class CrawlJobController {

    private final CrawlJobService crawlJobService;

    @GetMapping
    public ResponseEntity<List<CrawlJobDto>> getAllJobs(
            @RequestParam(required = false) String status
    ) {
        List<CrawlJobDto> jobs = status != null
                ? crawlJobService.getJobsByStatus(status)
                : crawlJobService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrawlJobDto> getJobById(@PathVariable Long id) {
        CrawlJobDto job = crawlJobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @PostMapping
    public ResponseEntity<CrawlJobDto> createJob(@Valid @RequestBody CreateCrawlJobRequest request) {
        CrawlJobDto job = crawlJobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CrawlJobDto> updateJobStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String errorMessage = body.get("errorMessage");
        CrawlJobDto job = crawlJobService.updateJobStatus(id, status, errorMessage);
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        crawlJobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-story/{storyId}")
    public ResponseEntity<List<CrawlJobDto>> getJobsByStory(
            @PathVariable Long storyId,
            @RequestParam(required = false) String jobType
    ) {
        List<CrawlJobDto> jobs = jobType != null
                ? crawlJobService.getJobsByStoryAndType(storyId, jobType)
                : crawlJobService.getJobsByStoryAndType(storyId, null);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/by-chapter/{chapterId}")
    public ResponseEntity<List<CrawlJobDto>> getJobsByChapter(@PathVariable Long chapterId) {
        List<CrawlJobDto> jobs = crawlJobService.getJobsByChapter(chapterId);
        return ResponseEntity.ok(jobs);
    }
}

