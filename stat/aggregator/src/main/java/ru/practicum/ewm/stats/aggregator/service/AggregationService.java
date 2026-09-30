package ru.practicum.ewm.stats.aggregator.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.aggregator.kafka.event.EventSimilarityProducer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;


@Service
public class AggregationService {

    private final ActionWeightService actionWeightService;

    private final EventSimilarityProducer eventSimilarityProducer;

    // eventId -> (userId -> максимальный вес)
    private final Map<Long, Map<Long, Double>> userWeights = new HashMap<>();

    // eventId -> сумма максимальных весов пользователей
    private final Map<Long, Double> eventWeightSums = new HashMap<>();

    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public AggregationService(
            ActionWeightService actionWeightService,
            EventSimilarityProducer eventSimilarityProducer
    ) {
        this.actionWeightService = actionWeightService;
        this.eventSimilarityProducer = eventSimilarityProducer;
    }

    public void process(UserActionAvro userAction) {
        long eventId = userAction.getEventId();
        long userId = userAction.getUserId();

        double newWeight = actionWeightService.getWeight(
                userAction.getActionType()
        );

        Map<Long, Double> eventWeights = userWeights.computeIfAbsent(
                eventId,
                id -> new HashMap<>()
        );

        double oldWeight = eventWeights.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            System.out.println(
                    "Weight not changed: eventId=" + eventId
                            + ", userId=" + userId
                            + ", oldWeight=" + oldWeight
                            + ", newWeight=" + newWeight
            );

            return;
        }

        double delta = newWeight - oldWeight;

        for (Map.Entry<Long, Map<Long, Double>> entry : userWeights.entrySet()) {

            long otherEventId = entry.getKey();

            if (otherEventId == eventId) {
                continue;
            }

            Map<Long, Double> otherEventWeights = entry.getValue();

            Double otherWeight = otherEventWeights.get(userId);

            if (otherWeight == null) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);

            double minDelta = newMin - oldMin;

            if (minDelta > 0) {
                addMinWeightDelta(
                        eventId,
                        otherEventId,
                        minDelta
                );
            }
        }

        eventWeights.put(userId, newWeight);

        eventWeightSums.merge(
                eventId,
                delta,
                Double::sum
        );

        for (Map.Entry<Long, Map<Long, Double>> entry : userWeights.entrySet()) {

            long otherEventId = entry.getKey();

            if (otherEventId == eventId) {
                continue;
            }

            if (!entry.getValue().containsKey(userId)) {
                continue;
            }

            double similarity = calculateSimilarity(
                    eventId,
                    otherEventId
            );

            long eventA = Math.min(eventId, otherEventId);
            long eventB = Math.max(eventId, otherEventId);

            EventSimilarityAvro eventSimilarity =
                    EventSimilarityAvro.newBuilder()
                            .setEventA(eventA)
                            .setEventB(eventB)
                            .setScore(similarity)
                            .setTimestamp(userAction.getTimestamp())
                            .build();

            eventSimilarityProducer.send(eventSimilarity);

            System.out.println(
                    "Similarity sent: " + eventSimilarity
            );
        }
    }

    private void addMinWeightDelta(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightSums
                .computeIfAbsent(first, id -> new HashMap<>())
                .merge(second, delta, Double::sum);
    }

    private double calculateSimilarity(long eventA, long eventB) {
        double sumA = eventWeightSums.getOrDefault(eventA, 0.0);
        double sumB = eventWeightSums.getOrDefault(eventB, 0.0);

        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }

        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        double minSum = minWeightSums
                .getOrDefault(first, Map.of())
                .getOrDefault(second, 0.0);

        return minSum / Math.sqrt(sumA * sumB);
    }
}
