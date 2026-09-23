package ru.practicum.interaction.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private Long initiatorId;
    private String state;
    private Integer participantLimit;
    private Integer confirmedRequests;
    private Boolean requestModeration;
}
