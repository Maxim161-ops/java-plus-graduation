package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CategoryDto;
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
            Map<Long, Double> ratingsMap) {

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events.stream()
                        .map(event -> toEventShortDto(event, ratingsMap))
                        .toList())
                .build();
    }

    private static EventShortDto toEventShortDto(
            EventShortResponse event,
            Map<Long, Double> ratingsMap) {

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .category(CategoryDto.builder()
                        .id(event.getCategoryId())
                        .name(event.getCategoryName())
                        .build())
                .initiator(event.getInitiator())
                .rating(ratingsMap.getOrDefault(event.getId(), 0.0))
                .confirmedRequests(event.getConfirmedRequests())
                .build();
    }
}