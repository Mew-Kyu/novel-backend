package com.graduate.novel.domain.history;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.ReadingHistoryMapper;
import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import com.graduate.novel.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository historyRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final ReadingHistoryMapper historyMapper;

    @Transactional(readOnly = true)
    public Page<ReadingHistoryDto> getReadingHistory(User currentUser, Pageable pageable) {
        Page<ReadingHistory> histories = historyRepository
                .findByUserIdOrderByLastReadAtDesc(currentUser.getId(), pageable);
        return histories.map(historyMapper::toDto);
    }

    @Transactional
    public ReadingHistoryDto updateReadingProgress(UpdateHistoryRequest request, User currentUser) {
        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + request.storyId()));

        Chapter chapter = null;
        if (request.chapterId() != null) {
            chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + request.chapterId()));
        }

        ReadingHistory history = historyRepository
                .findByUserIdAndStoryId(currentUser.getId(), request.storyId())
                .orElse(ReadingHistory.builder()
                        .user(currentUser)
                        .story(story)
                        .build());

        history.setChapter(chapter);
        if (request.progressPercent() != null) {
            history.setProgressPercent(request.progressPercent());
        }
        if (request.scrollOffset() != null) {
            history.setScrollOffset(request.scrollOffset());
        }

        history = historyRepository.save(history);
        return historyMapper.toDto(history);
    }
}
