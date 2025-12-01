package com.graduate.novel.controller;

import com.graduate.novel.domain.history.ReadingHistoryDto;
import com.graduate.novel.domain.history.ReadingHistoryService;
import com.graduate.novel.domain.history.UpdateHistoryRequest;
import com.graduate.novel.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private final ReadingHistoryService historyService;

    @GetMapping
    public ResponseEntity<Page<ReadingHistoryDto>> getReadingHistory(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "lastReadAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReadingHistoryDto> history = historyService.getReadingHistory(currentUser, pageable);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<ReadingHistoryDto> updateReadingProgress(
            @Valid @RequestBody UpdateHistoryRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReadingHistoryDto history = historyService.updateReadingProgress(request, currentUser);
        return ResponseEntity.ok(history);
    }
}
