package ru.practicum.ewm.stat.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserInteractionId implements Serializable {

    private Long userId;
    private Long eventId;
}
