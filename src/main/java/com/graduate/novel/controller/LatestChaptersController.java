package com.graduate.novel.controller;

import com.graduate.novel.domain.chapter.ChapterService;
import com.graduate.novel.domain.chapter.LatestChapterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class LatestChaptersController {

    private final ChapterService chapterService;

    /**
     * Get latest chapters across all stories
     */
    @GetMapping("/latest")
    public ResponseEntity<List<LatestChapterDto>> getLatestChapters(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<LatestChapterDto> chapters = chapterService.getLatestChapters(limit);
        return ResponseEntity.ok(chapters);
    }
}

