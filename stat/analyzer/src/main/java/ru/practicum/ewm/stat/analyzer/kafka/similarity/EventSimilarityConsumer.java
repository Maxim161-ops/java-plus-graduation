package ru.practicum.ewm.stat.analyzer.kafka.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.analyzer.service.EventSimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final EventSimilarityService eventSimilarityService;

    @KafkaListener(
            topics = "${analyzer.kafka.event-similarity.topic}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void consume(EventSimilarityAvro similarity) {
        eventSimilarityService.process(similarity);
    }
}