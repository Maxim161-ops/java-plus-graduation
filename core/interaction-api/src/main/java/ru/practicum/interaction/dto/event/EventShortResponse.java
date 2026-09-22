package ru.practicum.interaction.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.practicum.interaction.dto.user.UserShortDto;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortResponse {

    private Long id;

    private String title;

    private String annotation;

    private String eventDate;

    private Boolean paid;

    private Long categoryId;

    private String categoryName;

    private UserShortDto initiator;

    private Integer confirmedRequests;
}
