package com.graduate.novel.domain.crawljob;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.CrawlJobMapper;
import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.story.Story;
import com.graduate.novel.domain.story.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlJobService {

    private final CrawlJobRepository crawlJobRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CrawlJobMapper crawlJobMapper;

    @Transactional(readOnly = true)
    public List<CrawlJobDto> getAllJobs() {
        return crawlJobRepository.findAll().stream()
                .map(crawlJobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrawlJobDto> getJobsByStatus(String status) {
        return crawlJobRepository.findByStatus(status).stream()
                .map(crawlJobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CrawlJobDto getJobById(Long id) {
        CrawlJob job = crawlJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        return crawlJobMapper.toDto(job);
    }

    @Transactional
    public CrawlJobDto createJob(CreateCrawlJobRequest request) {
        CrawlJob job = CrawlJob.builder()
                .jobType(request.jobType())
                .status("PENDING")
                .attempts(0)
                .build();

        if (request.storyId() != null) {
            Story story = storyRepository.findById(request.storyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
            job.setStory(story);
        }

        if (request.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
            job.setChapter(chapter);
        }

        job = crawlJobRepository.save(job);
        return crawlJobMapper.toDto(job);
    }

    @Transactional
    public CrawlJobDto updateJobStatus(Long id, String status, String errorMessage) {
        CrawlJob job = crawlJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        job.setStatus(status);
        if (errorMessage != null) {
            job.setErrorMessage(errorMessage);
        }
        if ("PROCESSING".equals(status)) {
            job.setAttempts(job.getAttempts() + 1);
        }
        job.setUpdatedAt(LocalDateTime.now());

        job = crawlJobRepository.save(job);
        return crawlJobMapper.toDto(job);
    }

    @Transactional
    public void deleteJob(Long id) {
        CrawlJob job = crawlJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        crawlJobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public List<CrawlJobDto> getJobsByStoryAndType(Long storyId, String jobType) {
        return crawlJobRepository.findByStoryIdAndJobType(storyId, jobType).stream()
                .map(crawlJobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrawlJobDto> getJobsByChapter(Long chapterId) {
        return crawlJobRepository.findByChapterId(chapterId).stream()
                .map(crawlJobMapper::toDto)
                .toList();
    }
}

