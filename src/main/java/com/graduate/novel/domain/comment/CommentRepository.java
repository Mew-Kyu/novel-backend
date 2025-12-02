package com.graduate.novel.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByStoryIdOrderByCreatedAtDesc(Long storyId, Pageable pageable);

    Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByStoryId(Long storyId);
}

