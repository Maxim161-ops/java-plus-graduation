package ru.practicum.ewm.stat.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionEntity;
import ru.practicum.ewm.stat.analyzer.model.UserInteractionId;

import java.util.Collection;
import java.util.List;

public interface UserInteractionRepository
        extends JpaRepository<UserInteractionEntity, UserInteractionId> {

    List<UserInteractionEntity> findAllByEventIdIn(Collection<Long> eventIds);

    List<UserInteractionEntity> findAllByUserId(Long userId);

    List<UserInteractionEntity> findAllByUserIdOrderByInteractionTimeDesc(
            Long userId,
            Pageable pageable
    );

    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}