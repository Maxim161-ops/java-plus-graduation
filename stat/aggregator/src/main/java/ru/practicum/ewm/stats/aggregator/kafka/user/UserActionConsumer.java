package ru.practicum.ewm.stats.aggregator.kafka.user;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.service.AggregationService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionConsumer {

    private final AggregationService aggregationService;

    public UserActionConsumer(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @KafkaListener(topics = "stats.user-actions.v1")
    public void consume(UserActionAvro userAction) {
        System.out.println("Aggregator received: " + userAction);

        aggregationService.process(userAction);
    }
}
