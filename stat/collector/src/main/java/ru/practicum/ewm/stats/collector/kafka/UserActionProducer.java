package ru.practicum.ewm.stats.collector.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionProducer {

    private static final String TOPIC = "stats.user-actions.v1";

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    public UserActionProducer(KafkaTemplate<Long, UserActionAvro> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserActionAvro userAction) {
        kafkaTemplate.send(
                TOPIC,
                userAction.getEventId(),
                userAction
        );
    }
}
