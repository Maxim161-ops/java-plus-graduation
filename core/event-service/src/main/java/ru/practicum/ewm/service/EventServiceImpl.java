package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dao.CategoryRepository;
import ru.practicum.ewm.dao.EventCustomRepository;
import ru.practicum.ewm.dao.EventRepository;
import ru.practicum.ewm.dao.LocationRepository;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.stats.client.AnalyzerClient;
import ru.practicum.stats.client.CollectorClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int RECOMMENDATIONS_LIMIT = 10;

    private final EventRepository eventRepository;
    private final EventCustomRepository eventCustomRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;

    private final UserClient userClient;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;


    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        UserResponse user = userClient.getUser(userId);

        Event event = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(getEventDateWithValidation(dto.getEventDate()))
                .category(getCategoryByIdWithValidation(dto.getCategory()))
                .initiatorId(userId)
                .location(getLocation(dto.getLocation().getLat(), dto.getLocation().getLon()))
                .confirmedRequests(0)
                .participantLimit(dto.getParticipantLimit())
                .paid(dto.getPaid())
                .requestModeration(dto.getRequestModeration())
                .created(LocalDateTime.now())
                .state(EventState.PENDING)
                .build();

        log.info("Save event: {}", event);

        event = eventRepository.save(event);

        return EventMapper.toFullDto(event, 0.0, user);
    }


    @Override
    public EventFullDto getPrivateEvent(Long userId, Long eventId) {
        Event event = getEventIfExistWithOwnerValidation(eventId, userId);
        UserResponse user = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(event, getRating(eventId), user);
    }


    @Override
    @Transactional
    public EventFullDto updateEvent(
            Long userId,
            Long eventId,
            UpdateEventUserRequest request) {

        Event event = getEventIfExistWithOwnerValidation(eventId, userId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Event must not be published");
        }

        updateEventFieldsFromRequest(event, request);
        eventRepository.save(event);

        UserResponse user = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(event, getRating(eventId), user);
    }


    @Override
    public List<EventShortDto> getPrivateEvents(long userId, int from, int size) {
        UserResponse user = userClient.getUser(userId);

        List<Event> events =
                eventCustomRepository.findUserEventsWithPagination(userId, from, size);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> ratings = getRatings(getEventIds(events));

        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        ratings.getOrDefault(event.getId(), 0.0),
                        user
                ))
                .toList();
    }


    @Override
    public List<EventShortDto> getPublicEvents(EventSearchParams params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException(
                    "Дата начала не может быть позже даты окончания");
        }

        List<Event> events =
                eventCustomRepository.findPublicEventsWithPagination(
                        params,
                        rangeStart,
                        rangeEnd
                );

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> ratings = getRatings(getEventIds(events));
        Map<Long, UserResponse> users = getUsersMap(events);

        List<EventShortDto> result = events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        ratings.getOrDefault(event.getId(), 0.0),
                        users.get(event.getInitiatorId())
                ))
                .toList();

        if (params.getSort() == EventSort.RATING) {
            return result.stream()
                    .sorted(
                            Comparator.comparingDouble(EventShortDto::getRating)
                                    .reversed()
                    )
                    .toList();
        }

        return result;
    }


    @Override
    public EventFullDto getPublicEventById(Long eventId, Long userId) {
        Event event = getPublishedEvent(eventId);

        collectorClient.sendUserAction(
                userId,
                eventId,
                ActionTypeProto.ACTION_VIEW
        );

        UserResponse user = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(
                event,
                getRating(eventId),
                user
        );
    }


    @Override
    public List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params) {
        boolean usersEmpty = isEmpty(params.getUsers());
        boolean statesEmpty = isEmpty(params.getStates());
        boolean categoriesEmpty = isEmpty(params.getCategories());

        List<Event> events = eventCustomRepository.findEventsByAdminFilters(
                params,
                getIdsOrDefault(params.getUsers()),
                getStatesOrDefault(params.getStates()),
                getIdsOrDefault(params.getCategories()),
                getRangeStart(params.getRangeStart()),
                getRangeEnd(params.getRangeEnd()),
                usersEmpty,
                statesEmpty,
                categoriesEmpty
        );

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> ratings = getRatings(getEventIds(events));
        Map<Long, UserResponse> users = getUsersMap(events);

        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        ratings.getOrDefault(event.getId(), 0.0),
                        users.get(event.getInitiatorId())
                ))
                .toList();
    }


    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(
            Long eventId,
            UpdateEventAdminRequest request) {

        Event event = getEventIfExist(eventId);

        if (request.getEventDate() != null) {
            LocalDateTime eventDate = parseDateTimeOrNull(request.getEventDate());
            validateAdminEventDate(eventDate);
            event.setEventDate(eventDate);
        }

        if (request.getCategory() != null) {
            event.setCategory(
                    getCategoryByIdWithValidation(request.getCategory())
            );
        }

        if (request.getLocation() != null
                && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {

            event.setLocation(
                    getLocation(
                            request.getLocation().getLat(),
                            request.getLocation().getLon()
                    )
            );
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            updateAdminState(event, request.getStateAction());
        }

        eventRepository.save(event);

        UserResponse user = userClient.getUser(event.getInitiatorId());

        return EventMapper.toFullDto(
                event,
                getRating(eventId),
                user
        );
    }


    @Override
    public List<EventShortDto> getRecommendations(Long userId) {
        List<RecommendedEventProto> recommendations =
                analyzerClient
                        .getRecommendationsForUser(
                                userId,
                                RECOMMENDATIONS_LIMIT
                        )
                        .toList();

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        Map<Long, Event> events = eventRepository.findAllById(eventIds)
                .stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(
                        Event::getId,
                        Function.identity()
                ));

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserResponse> users =
                getUsersMap(new ArrayList<>(events.values()));

        Map<Long, Double> ratings =
                getRatings(new ArrayList<>(events.keySet()));

        return recommendations.stream()
                .map(recommendation -> {
                    Event event = events.get(recommendation.getEventId());

                    if (event == null) {
                        return null;
                    }

                    return EventMapper.toShortDto(
                            event,
                            ratings.getOrDefault(event.getId(), 0.0),
                            users.get(event.getInitiatorId())
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }


    @Override
    public void likeEvent(Long eventId, Long userId) {
        getPublishedEvent(eventId);

        if (!analyzerClient.hasUserInteraction(userId, eventId)) {
            throw new ValidationException(
                    "Пользователь не взаимодействовал с мероприятием " + eventId
            );
        }

        collectorClient.sendUserAction(
                userId,
                eventId,
                ActionTypeProto.ACTION_LIKE
        );
    }


    private double getRating(Long eventId) {
        return getRatings(List.of(eventId))
                .getOrDefault(eventId, 0.0);
    }


    private Map<Long, Double> getRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return analyzerClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore,
                        Math::max
                ));
    }


    private List<Long> getEventIds(List<Event> events) {
        return events.stream()
                .map(Event::getId)
                .toList();
    }


    private Map<Long, UserResponse> getUsersMap(List<Event> events) {
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        return userClient.getUsers(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserResponse::getId,
                        Function.identity()
                ));
    }


    private Event getPublishedEvent(Long eventId) {
        Event event = getEventIfExist(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(
                    "Событие с id=" + eventId + " не найдено"
            );
        }

        return event;
    }


    private Event getEventIfExist(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Field: eventId. Error: event не найден. Value: "
                                        + eventId
                        )
                );
    }


    private Event getEventIfExistWithOwnerValidation(
            long eventId,
            long userId) {

        Event event = getEventIfExist(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(
                    "Field: userId. Error: Initiator has another id. Value: "
                            + userId
            );
        }

        return event;
    }


    private Category getCategoryByIdWithValidation(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ValidationException(
                                "Категория с id=" + id + " не найдена"
                        )
                );
    }


    private Location getLocation(Float lat, Float lon) {
        return locationRepository.findByLatAndLon(lat, lon)
                .orElseGet(() ->
                        locationRepository.save(
                                new Location(lat, lon)
                        )
                );
    }


    private LocalDateTime getEventDateWithValidation(String date) {
        LocalDateTime eventDate =
                LocalDateTime.parse(date, FORMATTER);

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(
                    "Field: eventDate. Error: Начало события должно быть позже "
                            + LocalDateTime.now().plusHours(2)
                            + ". Value: "
                            + eventDate
            );
        }

        return eventDate;
    }


    private void updateEventFieldsFromRequest(
            Event event,
            UpdateEventUserRequest request) {

        if (request.getEventDate() != null) {
            event.setEventDate(
                    getEventDateWithValidation(request.getEventDate())
            );
        }

        if (request.getCategory() != null) {
            event.setCategory(
                    getCategoryByIdWithValidation(request.getCategory())
            );
        }

        if (request.getLocation() != null
                && request.getLocation().getLat() != null
                && request.getLocation().getLon() != null) {

            event.setLocation(
                    getLocation(
                            request.getLocation().getLat(),
                            request.getLocation().getLon()
                    )
            );
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            event.setState(
                    validateStateAction(
                            request.getStateAction(),
                            event.getState()
                    )
            );
        }
    }


    private EventState validateStateAction(
            String stateAction,
            EventState eventState) {

        return switch (stateAction.trim().toUpperCase()) {

            case "SEND_TO_REVIEW" -> {
                if (eventState != EventState.CANCELED) {
                    throw new ConflictException(
                            "Only pending or canceled events can be changed"
                    );
                }

                yield EventState.PENDING;
            }

            case "CANCEL_REVIEW" -> {
                if (eventState != EventState.PENDING) {
                    throw new ConflictException(
                            "Only pending or canceled events can be changed"
                    );
                }

                yield EventState.CANCELED;
            }

            default -> throw new ValidationException(
                    "Field: stateAction. Error: must be SEND_TO_REVIEW "
                            + "or CANCEL_REVIEW. Value: "
                            + stateAction
            );
        };
    }


    private void updateAdminState(
            Event event,
            String stateAction) {

        switch (stateAction.trim().toUpperCase()) {

            case "PUBLISH_EVENT" -> {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException(
                            "Cannot publish the event because it's not "
                                    + "in the right state: "
                                    + event.getState()
                    );
                }

                validateAdminEventDate(event.getEventDate());

                event.setState(EventState.PUBLISHED);
                event.setPublished(LocalDateTime.now());
            }

            case "REJECT_EVENT" -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException(
                            "Cannot reject the event because it's already published"
                    );
                }

                event.setState(EventState.CANCELED);
            }

            default -> throw new ValidationException(
                    "Field: stateAction. Error: must be PUBLISH_EVENT "
                            + "or REJECT_EVENT. Value: "
                            + stateAction
            );
        }
    }


    private void validateAdminEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException(
                    "Field: eventDate. Error: Начало события должно быть "
                            + "не ранее чем через час. Value: "
                            + eventDate
            );
        }
    }


    private LocalDateTime parseDateTimeOrNull(String date) {
        if (date == null) {
            return null;
        }

        if (date.isBlank()) {
            throw new ValidationException(
                    "Field: date. Error: date must not be blank"
            );
        }

        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ValidationException(
                    "Field: date. Error: date must have format "
                            + "yyyy-MM-dd HH:mm:ss. Value: "
                            + date
            );
        }
    }


    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }


    private List<Long> getIdsOrDefault(List<Long> ids) {
        return isEmpty(ids)
                ? List.of(-1L)
                : ids;
    }


    private List<String> getStatesOrDefault(List<String> states) {
        if (isEmpty(states)) {
            return List.of(EventState.PENDING.name());
        }

        return states.stream()
                .map(state ->
                        EventState.from(
                                state.trim().toUpperCase()
                        ).name()
                )
                .toList();
    }


    private LocalDateTime getRangeStart(String rangeStart) {
        return rangeStart == null
                ? LocalDateTime.of(1900, 1, 1, 0, 0)
                : parseDateTimeOrNull(rangeStart);
    }


    private LocalDateTime getRangeEnd(String rangeEnd) {
        return rangeEnd == null
                ? LocalDateTime.of(3000, 1, 1, 0, 0)
                : parseDateTimeOrNull(rangeEnd);
    }
}