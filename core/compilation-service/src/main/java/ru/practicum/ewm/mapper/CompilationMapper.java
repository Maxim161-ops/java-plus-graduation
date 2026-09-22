package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.interaction.dto.event.EventShortResponse;

import java.util.List;
import java.util.Map;

public class CompilationMapper {

    public static CompilationDto toCompilationDto(
            Compilation compilation,
            List<EventShortResponse> events,
            Map<Long, Long> viewsMap) {

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events.stream()
                        .map(event -> toEventShortDto(event, viewsMap))
                        .toList())
                .build();
    }

    private static EventShortDto toEventShortDto(
            EventShortResponse event,
            Map<Long, Long> viewsMap) {

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .category(ru.practicum.ewm.dto.CategoryDto.builder()
                        .id(event.getCategoryId())
                        .name(event.getCategoryName())
                        .build())
                .initiator(event.getInitiator())
                .views(viewsMap.getOrDefault(event.getId(), 0L))
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }
}