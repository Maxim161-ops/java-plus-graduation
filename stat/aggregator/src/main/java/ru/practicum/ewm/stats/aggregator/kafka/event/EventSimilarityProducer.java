package ru.practicum.ewm.stats.aggregator.kafka.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class EventSimilarityProducer {

    private static final String TOPIC = "stats.events-similarity.v1";

    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    public EventSimilarityProducer(
            KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(EventSimilarityAvro eventSimilarity) {
        kafkaTemplate.send(
                TOPIC,
                eventSimilarity.getEventA(),
                eventSimilarity
        );
    }
}
