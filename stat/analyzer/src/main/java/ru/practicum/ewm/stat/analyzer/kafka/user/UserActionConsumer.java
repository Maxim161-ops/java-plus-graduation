package ru.practicum.ewm.stat.analyzer.kafka.user;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stat.analyzer.service.UserInteractionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserInteractionService userInteractionService;

    @KafkaListener(
            topics = "${analyzer.kafka.user-actions.topic}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void consume(UserActionAvro userAction) {
        userInteractionService.process(userAction);
    }
}
