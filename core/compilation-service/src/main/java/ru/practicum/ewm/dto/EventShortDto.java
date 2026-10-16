package ru.practicum.ewm.dto;

import lombok.*;
import ru.practicum.interaction.dto.user.UserShortDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortDto {

    private Long id;
    private String title;
    private String annotation;
    private Integer confirmedRequests;
    private CategoryDto category;
    private String eventDate;
    private Double rating;
    private UserShortDto initiator;
    private Boolean paid;
}
