package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.*;

import java.util.List;

public interface EventService {

    EventFullDto addEvent(Long userId, NewEventDto dto);

    EventFullDto getPrivateEvent(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId,
                             UpdateEventUserRequest request);

    List<EventShortDto> getPrivateEvents(long userId, int from, int size);

    List<EventShortDto> getPublicEvents(EventSearchParams params);

    EventFullDto getPublicEventById(Long id, Long userId);

    List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params);

    EventFullDto updateEventByAdmin(Long eventId,
                                    UpdateEventAdminRequest request);

    List<EventShortDto> getRecommendations(Long userId);

    void likeEvent(Long eventId, Long userId);
}