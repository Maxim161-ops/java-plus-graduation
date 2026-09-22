package ru.practicum.ewm.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dao.CompilationRepository;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.CompilationsGetParams;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.QCompilation;
import ru.practicum.interaction.client.EventClient;
import ru.practicum.interaction.dto.event.EventShortResponse;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final StatClient statClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CompilationDto save(NewCompilationDto newCompilationDto) {
        log.info("Добавление новой подборки событий с названием: {}", newCompilationDto.getTitle());

        Set<Long> eventIds = new HashSet<>();

        if (newCompilationDto.getEvents() != null) {
            eventIds.addAll(newCompilationDto.getEvents());
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .eventIds(eventIds)
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);

        return getDto(savedCompilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("Удаление подборки событий с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Обновление информации о подборке событий с id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка id=" + compId + " не найдена"));

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getEvents() != null) {
            compilation.setEventIds(
                    new HashSet<>(updateCompilationRequest.getEvents())
            );
        }

        log.info("Обновление подборки: {}", compilation);
        Compilation updatedCompilation = compilationRepository.save(compilation);
        return getDto(updatedCompilation);
    }

    @Override
    public List<CompilationDto> getCompilations(CompilationsGetParams params) {
        log.info("Получение подборок событий (pinned={}, from={}, size={})",
                params.getPinned(), params.getFrom(), params.getSize());

        List<Compilation> compilations = findCompilations(params);

        Set<Long> eventIds = getEventIds(compilations);

        Map<Long, EventShortResponse> eventsById = getEventsById(eventIds);
        Map<Long, Long> viewsByEventId = getViewsByEventId(eventIds);

        return compilations.stream()
                .map(compilation -> toDto(
                        compilation,
                        eventsById,
                        viewsByEventId
                ))
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки событий с id: {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return getDto(compilation);
    }

    private BooleanExpression buildPredicate(CompilationsGetParams params) {
        QCompilation compilation = QCompilation.compilation;
        BooleanExpression predicate = null;

        if (params.getPinned() != null) {
            predicate = compilation.pinned.eq(params.getPinned());
        }

        return predicate == null ? compilation.isNotNull() : predicate;
    }

    private Map<Long, Long> getStatsByUris(List<String> uris) {

        List<ViewStatsDto> stats = statClient.getStat(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                false
        );

        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> viewsMap = new LinkedHashMap<>();
        for (ViewStatsDto dto : stats) {
            String uri = dto.getUri();
            Long id = Long.parseLong(
                    uri.substring(
                            uri.lastIndexOf('/') + 1));
            viewsMap.put(id, dto.getHits());
        }

        return viewsMap;
    }

    private CompilationDto getDto(Compilation compilation) {
        Set<Long> eventIds = compilation.getEventIds();

        Map<Long, EventShortResponse> eventsById = getEventsById(eventIds);
        Map<Long, Long> viewsByEventId = getViewsByEventId(eventIds);

        return toDto(compilation, eventsById, viewsByEventId);
    }


    private List<Compilation> findCompilations(CompilationsGetParams params) {
        int from = params.getFrom();
        int size = params.getSize();

        Pageable pageable = PageRequest.of(0, from + size);
        BooleanExpression predicate = buildPredicate(params);

        return compilationRepository.findAll(predicate, pageable)
                .getContent()
                .stream()
                .skip(from)
                .limit(size)
                .toList();
    }

    private Set<Long> getEventIds(List<Compilation> compilations) {
        return compilations.stream()
                .flatMap(compilation -> compilation.getEventIds().stream())
                .collect(Collectors.toSet());
    }

    private Map<Long, EventShortResponse> getEventsById(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return eventClient.getEvents(new ArrayList<>(eventIds))
                .stream()
                .collect(Collectors.toMap(
                        EventShortResponse::getId,
                        event -> event
                ));
    }

    private Map<Long, Long> getViewsByEventId(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventIds.stream()
                .map(eventId -> "/events/" + eventId)
                .toList();

        return getStatsByUris(uris);
    }

    private CompilationDto toDto(
            Compilation compilation,
            Map<Long, EventShortResponse> eventsById,
            Map<Long, Long> viewsByEventId) {

        List<EventShortResponse> events = compilation.getEventIds()
                .stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();

        return CompilationMapper.toCompilationDto(
                compilation,
                events,
                viewsByEventId
        );
    }
}