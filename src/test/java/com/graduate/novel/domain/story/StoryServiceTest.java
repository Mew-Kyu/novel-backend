package com.graduate.novel.domain.story;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.StoryMapper;
import com.graduate.novel.domain.chapter.Chapter;
import com.graduate.novel.domain.chapter.ChapterRepository;
import com.graduate.novel.domain.comment.CommentRepository;
import com.graduate.novel.domain.favorite.FavoriteRepository;
import com.graduate.novel.domain.genre.Genre;
import com.graduate.novel.domain.genre.GenreRepository;
import com.graduate.novel.domain.rating.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Story Service Tests")
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryMapper storyMapper;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private StoryService storyService;

    private Story testStory;
    private Genre testGenre;

    @BeforeEach
    void setUp() {
        testGenre = Genre.builder()
                .id(1L)
                .name("Fantasy")
                .description("Fantasy genre")
                .createdAt(LocalDateTime.now())
                .build();

        testStory = Story.builder()
                .id(1L)
                .title("Test Story")
                .rawTitle("テストストーリー")
                .translatedTitle("Test Story")
                .authorName("Test Author")
                .rawAuthorName("テスト著者")
                .translatedAuthorName("Test Author")
                .description("Test Description")
                .coverImageUrl("http://example.com/cover.jpg")
                .sourceUrl("http://example.com/story")
                .sourceSite("syosetu")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(100L)
                .featured(false)
                .status(StoryStatus.PUBLISHED)
                .genres(new HashSet<>(Collections.singletonList(testGenre)))
                .build();
    }

    @Test
    @DisplayName("Should get story with metadata successfully")
    void shouldGetStoryWithMetadataSuccessfully() {
        // Given
        Long storyId = 1L;
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(testStory));
        when(chapterRepository.countByStoryId(storyId)).thenReturn(10);
        when(ratingRepository.getAverageRatingByStoryId(storyId)).thenReturn(4.5);
        when(ratingRepository.getTotalRatingsByStoryId(storyId)).thenReturn(50L);
        when(commentRepository.countByStoryId(storyId)).thenReturn(25L);
        when(favoriteRepository.countByStoryId(storyId)).thenReturn(75L);

        Chapter latestChapter = Chapter.builder()
                .id(1L)
                .chapterIndex(10)
                .title("Chapter 10")
                .translatedTitle("Chapter 10")
                .updatedAt(LocalDateTime.now())
                .build();
        when(chapterRepository.findLatestByStoryId(eq(storyId), any(PageRequest.class)))
                .thenReturn(Collections.singletonList(latestChapter));

        // When
        StoryDetailDto result = storyService.getStoryWithMetadata(storyId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(storyId);
        assertThat(result.title()).isEqualTo("Test Story");
        assertThat(result.status()).isEqualTo(StoryStatus.PUBLISHED);
        assertThat(result.totalChapters()).isEqualTo(10);
        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.totalRatings()).isEqualTo(50L);
        assertThat(result.totalComments()).isEqualTo(25L);
        assertThat(result.totalFavorites()).isEqualTo(75L);
        assertThat(result.latestChapter()).isNotNull();
        assertThat(result.latestChapter().chapterIndex()).isEqualTo(10);

        verify(storyRepository).findById(storyId);
        verify(chapterRepository).countByStoryId(storyId);
        verify(ratingRepository).getAverageRatingByStoryId(storyId);
    }

    @Test
    @DisplayName("Should throw exception when story not found")
    void shouldThrowExceptionWhenStoryNotFound() {
        // Given
        Long storyId = 999L;
        when(storyRepository.findById(storyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> storyService.getStoryWithMetadata(storyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Story not found with id: " + storyId);

        verify(storyRepository).findById(storyId);
        verify(chapterRepository, never()).countByStoryId(anyLong());
    }

    @Test
    @DisplayName("Should increment view count successfully")
    void shouldIncrementViewCountSuccessfully() {
        // Given
        Long storyId = 1L;
        when(storyRepository.existsById(storyId)).thenReturn(true);

        // When
        storyService.incrementViewCount(storyId);

        // Then
        verify(storyRepository).existsById(storyId);
        verify(storyRepository).incrementViewCount(storyId);
    }

    @Test
    @DisplayName("Should throw exception when incrementing view count for non-existent story")
    void shouldThrowExceptionWhenIncrementingViewCountForNonExistentStory() {
        // Given
        Long storyId = 999L;
        when(storyRepository.existsById(storyId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> storyService.incrementViewCount(storyId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Story not found with id: " + storyId);

        verify(storyRepository).existsById(storyId);
        verify(storyRepository, never()).incrementViewCount(anyLong());
    }

    @Test
    @DisplayName("Should set featured status successfully")
    void shouldSetFeaturedStatusSuccessfully() {
        // Given
        Long storyId = 1L;
        StoryDto expectedDto = new StoryDto(
                1L, "Test Story", "テストストーリー", "Test Story",
                "Test Author", "テスト著者", "Test Author",
                "Description", "説明", "Description",
                "http://example.com/cover.jpg", "http://example.com/story",
                "syosetu", LocalDateTime.now(), StoryStatus.PUBLISHED, Collections.emptyList()
        );

        when(storyRepository.findById(storyId)).thenReturn(Optional.of(testStory));
        when(storyRepository.save(any(Story.class))).thenReturn(testStory);
        when(storyMapper.toDto(any(Story.class))).thenReturn(expectedDto);

        // When
        StoryDto result = storyService.setFeatured(storyId, true);

        // Then
        assertThat(result).isNotNull();
        verify(storyRepository).findById(storyId);
        verify(storyRepository).save(testStory);
        assertThat(testStory.getFeatured()).isTrue();
    }

    @Test
    @DisplayName("Should handle story with no chapters gracefully")
    void shouldHandleStoryWithNoChaptersGracefully() {
        // Given
        Long storyId = 1L;
        when(storyRepository.findById(storyId)).thenReturn(Optional.of(testStory));
        when(chapterRepository.countByStoryId(storyId)).thenReturn(0);
        when(ratingRepository.getAverageRatingByStoryId(storyId)).thenReturn(null);
        when(ratingRepository.getTotalRatingsByStoryId(storyId)).thenReturn(0L);
        when(commentRepository.countByStoryId(storyId)).thenReturn(0L);
        when(favoriteRepository.countByStoryId(storyId)).thenReturn(0L);
        when(chapterRepository.findLatestByStoryId(eq(storyId), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // When
        StoryDetailDto result = storyService.getStoryWithMetadata(storyId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalChapters()).isEqualTo(0);
        assertThat(result.averageRating()).isNull();
        assertThat(result.latestChapter()).isNull();
    }

    @Test
    @DisplayName("Should get story with different statuses")
    void shouldGetStoryWithDifferentStatuses() {
        // Test DRAFT status
        testStory.setStatus(StoryStatus.DRAFT);
        when(storyRepository.findById(1L)).thenReturn(Optional.of(testStory));
        when(chapterRepository.countByStoryId(anyLong())).thenReturn(0);
        when(ratingRepository.getAverageRatingByStoryId(anyLong())).thenReturn(null);
        when(ratingRepository.getTotalRatingsByStoryId(anyLong())).thenReturn(0L);
        when(commentRepository.countByStoryId(anyLong())).thenReturn(0L);
        when(favoriteRepository.countByStoryId(anyLong())).thenReturn(0L);
        when(chapterRepository.findLatestByStoryId(anyLong(), any())).thenReturn(Collections.emptyList());

        StoryDetailDto result = storyService.getStoryWithMetadata(1L);
        assertThat(result.status()).isEqualTo(StoryStatus.DRAFT);

        // Test COMPLETED status
        testStory.setStatus(StoryStatus.COMPLETED);
        result = storyService.getStoryWithMetadata(1L);
        assertThat(result.status()).isEqualTo(StoryStatus.COMPLETED);

        // Test ARCHIVED status
        testStory.setStatus(StoryStatus.ARCHIVED);
        result = storyService.getStoryWithMetadata(1L);
        assertThat(result.status()).isEqualTo(StoryStatus.ARCHIVED);
    }
}

