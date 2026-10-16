package ru.practicum.ewm.stat.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stat.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stat.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stat.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int NEIGHBORS_LIMIT = 10;

    private final UserInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getRecommendationsForUser(
            long userId,
            int maxResults) {

        if (maxResults <= 0) {
            return List.of();
        }

        // 1. Берём последние взаимодействия пользователя.
        List<UserInteractionEntity> recentInteractions =
                userInteractionRepository
                        .findAllByUserIdOrderByInteractionTimeDesc(
                                userId,
                                PageRequest.of(0, maxResults)
                        );

        if (recentInteractions.isEmpty()) {
            return List.of();
        }

        // 2. Берём ВСЕ мероприятия, с которыми пользователь уже взаимодействовал.
        // Они нужны, чтобы не рекомендовать уже просмотренное.
        List<UserInteractionEntity> allInteractions =
                userInteractionRepository.findAllByUserId(userId);

        Set<Long> interactedEventIds = allInteractions.stream()
                .map(UserInteractionEntity::getEventId)
                .collect(Collectors.toSet());

        Map<Long, UserInteractionEntity> interactionByEventId =
                allInteractions.stream()
                        .collect(Collectors.toMap(
                                UserInteractionEntity::getEventId,
                                Function.identity()
                        ));

        Set<Long> recentEventIds = recentInteractions.stream()
                .map(UserInteractionEntity::getEventId)
                .collect(Collectors.toSet());

        // 3. Находим пары similarity, где участвуют недавние мероприятия.
        List<EventSimilarityEntity> similarities =
                eventSimilarityRepository.findAllByEventAInOrEventBIn(
                        recentEventIds,
                        recentEventIds
                );

        // 4. Получаем новые мероприятия-кандидаты.

        Map<Long, Double> candidateScores = similarities.stream()
                .flatMap(similarity -> {
                    long eventA = similarity.getEventA();
                    long eventB = similarity.getEventB();

                    if (recentEventIds.contains(eventA)
                            && !interactedEventIds.contains(eventB)) {

                        return Map.of(
                                eventB,
                                similarity.getScore()
                        ).entrySet().stream();
                    }

                    if (recentEventIds.contains(eventB)
                            && !interactedEventIds.contains(eventA)) {

                        return Map.of(
                                eventA,
                                similarity.getScore()
                        ).entrySet().stream();
                    }

                    return Stream.empty();
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Math::max
                ));

        // 5. Берём N наиболее похожих кандидатов.
        List<Long> candidates = candidateScores.entrySet()
                .stream()
                .sorted(
                        Map.Entry.<Long, Double>comparingByValue()
                                .reversed()
                )
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        // 6. Для каждого кандидата рассчитываем предсказанную оценку.
        return candidates.stream()
                .map(candidateEventId ->
                        predictScore(
                                candidateEventId,
                                interactedEventIds,
                                interactionByEventId
                        )
                )
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparingDouble(
                                RecommendedEventProto::getScore
                        ).reversed()
                )
                .limit(maxResults)
                .toList();
    }

    private RecommendedEventProto predictScore(
            long candidateEventId,
            Set<Long> interactedEventIds,
            Map<Long, UserInteractionEntity> interactionByEventId) {

        // Все мероприятия, похожие на кандидата.
        List<EventSimilarityEntity> similarities =
                eventSimilarityRepository.findAllByEventAOrEventB(
                        candidateEventId,
                        candidateEventId
                );

        // Оставляем только те, с которыми пользователь уже взаимодействовал,
        // сортируем по similarity и берём K ближайших соседей.
        List<EventSimilarityEntity> neighbors = similarities.stream()
                .filter(similarity -> {
                    long neighborId =
                            getOtherEventId(
                                    similarity,
                                    candidateEventId
                            );

                    return interactedEventIds.contains(neighborId);
                })
                .sorted(
                        Comparator.comparingDouble(
                                EventSimilarityEntity::getScore
                        ).reversed()
                )
                .limit(NEIGHBORS_LIMIT)
                .toList();

        if (neighbors.isEmpty()) {
            return null;
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarityEntity similarity : neighbors) {

            long neighborEventId =
                    getOtherEventId(
                            similarity,
                            candidateEventId
                    );

            double userWeight =
                    interactionByEventId
                            .get(neighborEventId)
                            .getWeight();

            double similarityScore =
                    similarity.getScore();

            weightedSum += userWeight * similarityScore;
            similaritySum += similarityScore;
        }

        if (similaritySum == 0.0) {
            return null;
        }

        double predictedScore =
                weightedSum / similaritySum;

        return RecommendedEventProto.newBuilder()
                .setEventId(candidateEventId)
                .setScore(predictedScore)
                .build();
    }

    private long getOtherEventId(
            EventSimilarityEntity similarity,
            long eventId) {

        return similarity.getEventA() == eventId
                ? similarity.getEventB()
                : similarity.getEventA();
    }
}
