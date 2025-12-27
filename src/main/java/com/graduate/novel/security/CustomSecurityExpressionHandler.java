package com.graduate.novel.security;

import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Custom security expression handler for checking resource ownership.
 * Used in @PreAuthorize annotations to enforce that:
 * - ADMIN can modify any resource
 * - MODERATOR can only modify resources they created
 */
@Component("securityExpressionHandler")
@RequiredArgsConstructor
public class CustomSecurityExpressionHandler {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    /**
     * Check if the current user can modify (update/delete) a story.
     * @param authentication Current authentication
     * @param storyId Story ID to check
     * @return true if user is ADMIN or is the creator of the story
     */
    public boolean canModifyStory(Authentication authentication, Long storyId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // ADMIN can modify any story
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }

        // MODERATOR can only modify their own stories
        if (hasRole(authentication, "MODERATOR")) {
            User currentUser = (User) authentication.getPrincipal();
            Story story = storyRepository.findById(storyId).orElse(null);

            if (story == null) {
                return false; // Story not found
            }

            // Check if the current user is the creator
            return story.getCreatedBy() != null &&
                   story.getCreatedBy().equals(currentUser.getId());
        }

        return false;
    }

    /**
     * Check if the current user can modify (update/delete) a chapter.
     * @param authentication Current authentication
     * @param storyId Story ID (for validation)
     * @param chapterId Chapter ID to check
     * @return true if user is ADMIN or is the creator of the chapter
     */
    public boolean canModifyChapter(Authentication authentication, Long storyId, Long chapterId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // ADMIN can modify any chapter
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }

        // MODERATOR can only modify their own chapters
        if (hasRole(authentication, "MODERATOR")) {
            User currentUser = (User) authentication.getPrincipal();
            Chapter chapter = chapterRepository.findById(chapterId).orElse(null);

            if (chapter == null) {
                return false; // Chapter not found
            }

            // Verify chapter belongs to the story
            if (!chapter.getStory().getId().equals(storyId)) {
                return false;
            }

            // Check if the current user is the creator
            return chapter.getCreatedBy() != null &&
                   chapter.getCreatedBy().equals(currentUser.getId());
        }

        return false;
    }

    /**
     * Check if authentication has a specific role.
     * Note: Spring Security roles have "ROLE_" prefix.
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_" + role));
    }
}

