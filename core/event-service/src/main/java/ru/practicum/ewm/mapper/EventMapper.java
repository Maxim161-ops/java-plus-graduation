package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.Event;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.interaction.dto.user.UserShortDto;

import java.time.format.DateTimeFormatter;

public class EventMapper {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static EventFullDto toFullDto(Event event,
                                         Double rating,
                                         UserResponse user) {
        return EventFullDto.builder()
                .id(event.getId())
                .paid(event.getPaid())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .state(event.getState())
                .title(event.getTitle())
                .initiator(new UserShortDto(user.getId(), user.getName()))
                .location(new LocationDto(
                        event.getLocation().getLat(),
                        event.getLocation().getLon()))
                .eventDate(event.getEventDate().format(FORMATTER))
                .createdOn(event.getCreated().format(FORMATTER))
                .publishedOn(event.getPublished() == null
                        ? null
                        : event.getPublished().format(FORMATTER))
                .description(event.getDescription())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .annotation(event.getAnnotation())
                .rating(rating == null ? 0.0 : rating)
                .build();
    }

    public static EventShortDto toShortDto(Event event,
                                           Double rating,
                                           UserResponse user) {
        return EventShortDto.builder()
                .id(event.getId())
                .paid(event.getPaid())
                .category(CategoryMapper.toDto(event.getCategory()))
                .title(event.getTitle())
                .confirmedRequests(event.getConfirmedRequests())
                .initiator(new UserShortDto(user.getId(), user.getName()))
                .eventDate(event.getEventDate().format(FORMATTER))
                .annotation(event.getAnnotation())
                .rating(rating == null ? 0.0 : rating)
                .build();
    }
}