package com.graduate.novel.domain.story;

import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.StoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

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
}
