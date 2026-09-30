package ru.practicum.ewm.stats.aggregator.service;

import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Service
public class ActionWeightService {

    public double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}