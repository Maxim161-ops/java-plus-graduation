package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Event;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.event.ConfirmedRequestsUpdateRequest;
import ru.practicum.interaction.dto.event.EventResponse;
import ru.practicum.interaction.dto.event.EventShortResponse;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.interaction.dto.user.UserShortDto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;
    private final UserClient userClient;

    @GetMapping("/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException("Event id: " + eventId + " not found."));

        return EventResponse.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .confirmedRequests(event.getConfirmedRequests())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    @PatchMapping("/{eventId}/confirmed-requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateConfirmedRequests(
            @PathVariable Long eventId,
            @RequestBody ConfirmedRequestsUpdateRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException("Event id: " + eventId + " not found."));

        event.setConfirmedRequests(
                event.getConfirmedRequests() + request.getDelta()
        );

        eventRepository.save(event);
    }

    @GetMapping
    public List<EventShortResponse> getEvents(@RequestParam List<Long> ids) {

        List<Event> events = eventRepository.findAllById(ids);

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        Map<Long, UserResponse> users = userClient.getUsers(userIds).stream()
                .collect(Collectors.toMap(
                        UserResponse::getId,
                        user -> user
                ));

        return events.stream()
                .map(event -> {
                    UserResponse user = users.get(event.getInitiatorId());

                    return EventShortResponse.builder()
                            .id(event.getId())
                            .title(event.getTitle())
                            .annotation(event.getAnnotation())
                            .eventDate(event.getEventDate()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .paid(event.getPaid())
                            .categoryId(event.getCategory().getId())
                            .categoryName(event.getCategory().getName())
                            .initiator(new UserShortDto(
                                    user.getId(),
                                    user.getName()
                            ))
                            .confirmedRequests(event.getConfirmedRequests())
                            .build();
                })
                .toList();
    }
}
