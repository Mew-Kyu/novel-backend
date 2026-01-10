package com.graduate.novel.domain.recommendation.coldstart;

import com.graduate.novel.domain.story.StoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColdStartService {

    private final List<ColdStartStrategy> strategies;

    public List<StoryDto> getRecommendations(Long userId, int limit) {
        log.info("Getting cold-start recommendations for user {}", userId);

        List<ColdStartStrategy> applicableStrategies = strategies.stream()
            .filter(strategy -> strategy.isApplicable(userId))
            .sorted(Comparator.comparingInt(ColdStartStrategy::getPriority).reversed())
            .collect(Collectors.toList());

        if (applicableStrategies.isEmpty()) {
            log.warn("No applicable cold-start strategy found for user {}", userId);
            return Collections.emptyList();
        }

        ColdStartStrategy selectedStrategy = applicableStrategies.get(0);
        log.info("Selected cold-start strategy: {} (priority: {})",
            selectedStrategy.getName(), selectedStrategy.getPriority());

        return selectedStrategy.getRecommendations(userId, limit);
    }

    public List<StoryDto> getMixedRecommendations(Long userId, int limit) {
        log.info("Getting mixed cold-start recommendations for user {}", userId);

        List<ColdStartStrategy> applicableStrategies = strategies.stream()
            .filter(strategy -> strategy.isApplicable(userId))
            .sorted(Comparator.comparingInt(ColdStartStrategy::getPriority).reversed())
            .limit(3)
            .collect(Collectors.toList());

        if (applicableStrategies.isEmpty()) {
            return Collections.emptyList();
        }

        int perStrategy = limit / applicableStrategies.size();
        int remainder = limit % applicableStrategies.size();

        Set<Long> seenIds = new HashSet<>();
        List<StoryDto> allRecommendations = new ArrayList<>();

        for (int i = 0; i < applicableStrategies.size(); i++) {
            ColdStartStrategy strategy = applicableStrategies.get(i);
            int strategyLimit = perStrategy + (i < remainder ? 1 : 0);

            try {
                List<StoryDto> strategyRecs = strategy.getRecommendations(userId, strategyLimit * 2);

                strategyRecs.stream()
                    .filter(story -> seenIds.add(story.id()))
                    .limit(strategyLimit)
                    .forEach(allRecommendations::add);

                log.debug("Added {} recommendations from {}",
                    strategyRecs.size(), strategy.getName());
            } catch (Exception e) {
                log.error("Error getting recommendations from {}: {}",
                    strategy.getName(), e.getMessage());
            }
        }

        return allRecommendations.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public boolean isUserColdStart(Long userId) {
        return strategies.stream()
            .anyMatch(strategy -> strategy.isApplicable(userId));
    }

    public String getRecommendedStrategy(Long userId) {
        return strategies.stream()
            .filter(strategy -> strategy.isApplicable(userId))
            .max(Comparator.comparingInt(ColdStartStrategy::getPriority))
            .map(ColdStartStrategy::getName)
            .orElse("NONE");
    }
}

