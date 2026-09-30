package ru.practicum.ewm.stat.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionId;
import ru.practicum.ewm.stat.analyzer.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInteractionService {

    private final UserInteractionRepository repository;

    @Transactional
    public void process(UserActionAvro userAction) {

        double newWeight = getWeight(userAction.getActionType());

        UserInteractionId id = new UserInteractionId(
                userAction.getUserId(),
                userAction.getEventId()
        );

        UserInteractionEntity interaction = repository.findById(id)
                .orElse(null);

        if (interaction == null) {
            interaction = UserInteractionEntity.builder()
                    .userId(userAction.getUserId())
                    .eventId(userAction.getEventId())
                    .weight(newWeight)
                    .interactionTime(userAction.getTimestamp())
                    .build();

            repository.save(interaction);
            return;
        }

        if (newWeight > interaction.getWeight()) {
            interaction.setWeight(newWeight);
            interaction.setInteractionTime(userAction.getTimestamp());

            repository.save(interaction);
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {

        Map<Long, Double> interactionCounts = repository
                .findAllByEventIdIn(eventIds)
                .stream()
                .collect(Collectors.groupingBy(
                        UserInteractionEntity::getEventId,
                        Collectors.summingDouble(UserInteractionEntity::getWeight)
                ));

        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(interactionCounts.getOrDefault(eventId, 0.0))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasInteraction(long userId, long eventId) {
        return repository.existsByUserIdAndEventId(userId, eventId);
    }
}
