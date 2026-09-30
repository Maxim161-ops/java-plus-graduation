package ru.practicum.ewm.stat.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.stat.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stat.analyzer.model.EventSimilarityId;

import java.util.Collection;
import java.util.List;

public interface EventSimilarityRepository
        extends JpaRepository<EventSimilarityEntity, EventSimilarityId> {

    List<EventSimilarityEntity> findAllByEventAOrEventB(
            Long eventA,
            Long eventB
    );

    List<EventSimilarityEntity> findAllByEventAInOrEventBIn(
            Collection<Long> eventA,
            Collection<Long> eventB
    );
}
