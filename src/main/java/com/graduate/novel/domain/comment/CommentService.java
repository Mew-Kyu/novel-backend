package com.graduate.novel.domain.comment;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.CommentMapper;
import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentDto createComment(Long userId, CreateCommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + request.storyId()));

        Comment comment = Comment.builder()
                .user(user)
                .story(story)
                .content(request.content())
                .build();

        comment = commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Allow update if user owns the comment OR user is ADMIN/MODERATOR
        if (!comment.getUser().getId().equals(userId) && !hasModeratorAccess(user)) {
            throw new AccessDeniedException("You can only update your own comments");
        }

        comment.setContent(request.content());
        comment = commentRepository.save(comment);

        return commentMapper.toDto(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Allow deletion if user owns the comment OR user is ADMIN/MODERATOR
        if (!comment.getUser().getId().equals(userId) && !hasModeratorAccess(user)) {
            throw new AccessDeniedException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    /**
     * Check if user has ADMIN or MODERATOR role
     */
    private boolean hasModeratorAccess(User user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsByStory(Long storyId, Pageable pageable) {
        // Verify story exists
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + storyId));

        return commentRepository.findByStoryIdOrderByCreatedAtDesc(storyId, pageable)
                .map(commentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsByUser(Long userId, Pageable pageable) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(commentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        return commentMapper.toDto(comment);
    }

    @Transactional(readOnly = true)
    public long getCommentCountByStory(Long storyId) {
        return commentRepository.countByStoryId(storyId);
    }
}

