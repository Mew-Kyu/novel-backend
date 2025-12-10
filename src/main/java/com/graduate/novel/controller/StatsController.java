package com.graduate.novel.controller;

import com.graduate.novel.domain.stats.StatsSummaryDto;
import com.graduate.novel.domain.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public ResponseEntity<StatsSummaryDto> getSummary() {
        StatsSummaryDto summary = statsService.getSummary();
        return ResponseEntity.ok(summary);
    }
}

