package com.graduate.novel.domain.story;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.comment.CommentRepository;
import com.graduate.novel.domain.favorite.FavoriteRepository;
import com.graduate.novel.domain.genre.GenreDto;
import com.graduate.novel.domain.rating.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final ChapterRepository chapterRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional(readOnly = true)
    public Page<StoryDto> getStories(String keyword, Pageable pageable) {
        Page<Story> stories;
        if (keyword != null && !keyword.isEmpty()) {
            stories = storyRepository.findByTitleContainingOrDescriptionContaining(
                    keyword, keyword, pageable);
        } else {
            stories = storyRepository.findAll(pageable);
        }
        return stories.map(storyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StoryDto> getStoriesByGenre(Long genreId, Pageable pageable) {
        Page<Story> stories = storyRepository.findByGenreId(genreId, pageable);
        return stories.map(storyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StoryDto> getStoriesByGenreName(String genreName, Pageable pageable) {
        Page<Story> stories = storyRepository.findByGenreName(genreName, pageable);
        return stories.map(storyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public StoryDto getStoryById(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));
        return storyMapper.toDto(story);
    }

    @Transactional
    public StoryDto createStory(CreateStoryRequest request) {
        Story story = storyMapper.toEntity(request);
        story = storyRepository.save(story);
        return storyMapper.toDto(story);
    }

    @Transactional
    public StoryDto updateStory(Long id, UpdateStoryRequest request) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));

        storyMapper.updateEntity(request, story);
        story = storyRepository.save(story);
        return storyMapper.toDto(story);
    }

    @Transactional
    public void deleteStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));
        storyRepository.delete(story);
    }

    @Transactional
    public void updateEmbedding(Long id, float[] embedding) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));

        // Convert float[] to PostgreSQL vector format string
        String vectorString = convertFloatArrayToVectorString(embedding);
        story.setEmbedding(vectorString);
        storyRepository.save(story);
    }

    /**
     * Convert float array to PostgreSQL vector format string
     */
    private String convertFloatArrayToVectorString(float[] array) {
        if (array == null || array.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    // ========== Homepage Features ==========

    /**
     * Get story with full metadata
     */
    @Transactional(readOnly = true)
    public StoryDetailDto getStoryWithMetadata(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));
        return mapToDetailDto(story);
    }

    /**
     * Get stories with metadata
     */
    @Transactional(readOnly = true)
    public Page<StoryDetailDto> getStoriesWithMetadata(String keyword, Pageable pageable) {
        Page<Story> stories;
        if (keyword != null && !keyword.isEmpty()) {
            stories = storyRepository.findByTitleContainingOrDescriptionContaining(
                    keyword, keyword, pageable);
        } else {
            stories = storyRepository.findAll(pageable);
        }
        return stories.map(this::mapToDetailDto);
    }

    /**
     * Get stories by genre with metadata
     */
    @Transactional(readOnly = true)
    public Page<StoryDetailDto> getStoriesByGenreWithMetadata(Long genreId, Pageable pageable) {
        Page<Story> stories = storyRepository.findByGenreId(genreId, pageable);
        return stories.map(this::mapToDetailDto);
    }

    /**
     * Get stories by genre name with metadata
     */
    @Transactional(readOnly = true)
    public Page<StoryDetailDto> getStoriesByGenreNameWithMetadata(String genreName, Pageable pageable) {
        Page<Story> stories = storyRepository.findByGenreName(genreName, pageable);
        return stories.map(this::mapToDetailDto);
    }

    /**
     * Get featured stories
     */
    @Transactional(readOnly = true)
    public List<StoryDetailDto> getFeaturedStories(int limit) {
        Page<Story> stories = storyRepository.findFeaturedStories(PageRequest.of(0, limit));
        return stories.stream()
                .map(this::mapToDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Get trending stories (updated recently with high view count)
     */
    @Transactional(readOnly = true)
    public List<StoryDetailDto> getTrendingStories(int limit, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Page<Story> stories = storyRepository.findTrendingStories(since, PageRequest.of(0, limit));
        return stories.stream()
                .map(this::mapToDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Increment view count
     */
    @Transactional
    public void incrementViewCount(Long storyId) {
        if (!storyRepository.existsById(storyId)) {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        }
        storyRepository.incrementViewCount(storyId);
    }

    /**
     * Set featured status (Admin only)
     */
    @Transactional
    public StoryDto setFeatured(Long id, boolean featured) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));
        story.setFeatured(featured);
        story = storyRepository.save(story);
        return storyMapper.toDto(story);
    }

    /**
     * Map Story to StoryDetailDto with metadata
     */
    private StoryDetailDto mapToDetailDto(Story story) {
        // Get metadata
        Integer totalChapters = chapterRepository.countByStoryId(story.getId());
        Double averageRating = ratingRepository.getAverageRatingByStoryId(story.getId());
        Long totalRatings = ratingRepository.getTotalRatingsByStoryId(story.getId());
        Long totalComments = commentRepository.countByStoryId(story.getId());
        Long totalFavorites = favoriteRepository.countByStoryId(story.getId());

        // Get latest chapter
        List<Chapter> latestChapters = chapterRepository.findLatestByStoryId(
                story.getId(), PageRequest.of(0, 1));
        StoryDetailDto.LatestChapterInfo latestChapter = null;
        if (!latestChapters.isEmpty()) {
            Chapter chapter = latestChapters.get(0);
            latestChapter = new StoryDetailDto.LatestChapterInfo(
                    chapter.getId(),
                    chapter.getChapterIndex(),
                    chapter.getTitle(),
                    chapter.getTranslatedTitle(),
                    chapter.getUpdatedAt()
            );
        }

        // Map genres
        List<GenreDto> genres = story.getGenres().stream()
                .map(genre -> new GenreDto(
                        genre.getId(),
                        genre.getName(),
                        genre.getDescription(),
                        genre.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new StoryDetailDto(
                story.getId(),
                story.getTitle(),
                story.getRawTitle(),
                story.getTranslatedTitle(),
                story.getAuthorName(),
                story.getRawAuthorName(),
                story.getTranslatedAuthorName(),
                story.getDescription(),
                story.getRawDescription(),
                story.getTranslatedDescription(),
                story.getCoverImageUrl(),
                story.getSourceUrl(),
                story.getSourceSite(),
                story.getCreatedAt(),
                story.getUpdatedAt(),
                story.getViewCount(),
                story.getFeatured(),
                totalChapters,
                averageRating,
                totalRatings,
                totalComments,
                totalFavorites,
                genres,
                latestChapter
        );
    }
}
