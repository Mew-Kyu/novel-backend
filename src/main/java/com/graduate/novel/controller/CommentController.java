package com.graduate.novel.controller;

import com.graduate.novel.domain.comment.*;
import com.graduate.novel.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentDto comment = commentService.createComment(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentDto comment = commentService.updateComment(user.getId(), commentId, request);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId) {
        commentService.deleteComment(user.getId(), commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long commentId) {
        CommentDto comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<Page<CommentDto>> getCommentsByStory(
            @PathVariable Long storyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentDto> comments = commentService.getCommentsByStory(storyId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/story/{storyId}/count")
    public ResponseEntity<Long> getCommentCountByStory(@PathVariable Long storyId) {
        long count = commentService.getCommentCountByStory(storyId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/me")
    public ResponseEntity<Page<CommentDto>> getMyComments(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentDto> comments = commentService.getCommentsByUser(user.getId(), pageable);
        return ResponseEntity.ok(comments);
    }
}

