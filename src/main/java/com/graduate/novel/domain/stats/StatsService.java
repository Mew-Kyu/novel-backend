package com.graduate.novel.domain.stats;

import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.genre.GenreRepository;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StoryRepository storyRepository;
    private final GenreRepository genreRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StatsSummaryDto getSummary() {
        Long totalStories = storyRepository.count();
        Long totalGenres = genreRepository.count();
        Long totalChapters = chapterRepository.count();
        Long totalUsers = userRepository.count();
        Long totalViews = storyRepository.getTotalViewCount();

        return new StatsSummaryDto(
                totalStories,
                totalGenres,
                totalChapters,
                totalUsers,
                totalViews
        );
    }
}

