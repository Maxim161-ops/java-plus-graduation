package ru.practicum.ewm.stat.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stat.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stat.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stat.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stat.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private final EventSimilarityRepository repository;
    private final UserInteractionRepository userInteractionRepository;

    @Transactional
    public void process(EventSimilarityAvro similarity) {

        EventSimilarityId id = new EventSimilarityId(
                similarity.getEventA(),
                similarity.getEventB()
        );

        EventSimilarityEntity entity = repository.findById(id)
                .orElseGet(() -> EventSimilarityEntity.builder()
                        .eventA(similarity.getEventA())
                        .eventB(similarity.getEventB())
                        .build()
                );

        entity.setScore(similarity.getScore());
        entity.setCalculationTime(similarity.getTimestamp());

        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getSimilarEvents(
            long eventId,
            long userId,
            int maxResults) {

        Set<Long> interactedEventIds = userInteractionRepository
                .findAllByUserId(userId)
                .stream()
                .map(UserInteractionEntity::getEventId)
                .collect(Collectors.toSet());

        return repository
                .findAllByEventAOrEventB(eventId, eventId)
                .stream()

                .map(similarity -> {
                    long similarEventId =
                            similarity.getEventA() == eventId
                                    ? similarity.getEventB()
                                    : similarity.getEventA();

                    return RecommendedEventProto.newBuilder()
                            .setEventId(similarEventId)
                            .setScore(similarity.getScore())
                            .build();
                })

                .filter(event ->
                        !interactedEventIds.contains(event.getEventId())
                )

                .sorted((first, second) ->
                        Double.compare(
                                second.getScore(),
                                first.getScore()
                        )
                )

                .limit(maxResults)
                .toList();
    }
}
